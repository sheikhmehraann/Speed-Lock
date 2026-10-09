"""
abi_validator.py

Deep binary verification of Loadable Kernel Module (LKM) ELF artifacts
against target Linux kernel ABI and symbol version (modversions) requirements.
Ensures metadata-only changes (like string patching) are never conflated with genuine ABI compatibility.
"""

import os
import re
import struct
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple, Any

@dataclass
class AbiVerificationResult:
    module_path: str
    file_size_bytes: int = 0
    vermagic: Optional[str] = None
    vermagic_matches: bool = False
    has_modversions: bool = False
    imported_symbols: List[str] = field(default_factory=list)
    symbol_crcs: Dict[str, str] = field(default_factory=dict)
    matched_crcs: Dict[str, str] = field(default_factory=dict)
    mismatched_crcs: Dict[str, Tuple[str, str]] = field(default_factory=dict) # sym: (expected, found)
    missing_crcs: List[str] = field(default_factory=list)
    is_abi_compatible: bool = False
    rejection_reasons: List[str] = field(default_factory=list)

class AbiValidator:
    @staticmethod
    def parse_module(ko_path: str) -> Optional[Dict[str, Any]]:
        """Parses ELF sections, vermagic, and __versions table from a 64-bit ELF .ko file."""
        if not os.path.exists(ko_path):
            return None
        with open(ko_path, "rb") as f:
            data = f.read()

        if len(data) < 64 or data[:4] != b"\x7fELF" or data[4] != 2:
            return {"error": "Invalid 64-bit ELF binary"}

        # Extract vermagic
        m_vm = re.search(rb"vermagic=([^\x00]+)", data)
        vermagic = m_vm.group(1).decode("latin1") if m_vm else None

        e_shoff = struct.unpack_from("<Q", data, 40)[0]
        e_shentsize = struct.unpack_from("<H", data, 58)[0]
        e_shnum = struct.unpack_from("<H", data, 60)[0]
        e_shstrndx = struct.unpack_from("<H", data, 62)[0]

        strtab_hdr = data[e_shoff + e_shstrndx * e_shentsize : e_shoff + (e_shstrndx + 1) * e_shentsize]
        sh_name, sh_type, sh_flags, sh_addr, strtab_off, strtab_sz = struct.unpack_from("<IIQQQQ", strtab_hdr, 0)
        shstrtab = data[strtab_off : strtab_off + strtab_sz]

        sections = {}
        symtab_hdr = None
        strtab_sec_hdr = None
        versions_hdr = None

        for i in range(e_shnum):
            hdr = data[e_shoff + i * e_shentsize : e_shoff + (i + 1) * e_shentsize]
            sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size = struct.unpack_from("<IIQQQQ", hdr, 0)
            sec_name = shstrtab[sh_name:].split(b"\x00")[0].decode("latin1")
            sections[sec_name] = (sh_offset, sh_size, sh_type)
            if sec_name == ".symtab":
                symtab_hdr = hdr
            elif sec_name == ".strtab":
                strtab_sec_hdr = hdr
            elif sec_name == "__versions":
                versions_hdr = hdr

        # Parse imported symbols
        imported = []
        if symtab_hdr and strtab_sec_hdr:
            sym_off, sym_sz = struct.unpack_from("<QQ", symtab_hdr, 24)
            str_off, str_sz = struct.unpack_from("<QQ", strtab_sec_hdr, 24)
            strtab = data[str_off : str_off + str_sz]
            num_syms = sym_sz // 24
            for i in range(num_syms):
                sym_entry = data[sym_off + i * 24 : sym_off + (i + 1) * 24]
                st_name, st_info, st_other, st_shndx, st_value, st_size = struct.unpack_from("<IBBHQQ", sym_entry, 0)
                name = strtab[st_name:].split(b"\x00")[0].decode("latin1")
                if st_shndx == 0 and name:
                    imported.append(name)

        # Parse __versions CRCs
        symbol_crcs = {}
        if versions_hdr:
            v_off, v_sz = struct.unpack_from("<QQ", versions_hdr, 24)
            if v_sz > 0:
                vdata = data[v_off : v_off + v_sz]
                entry_sz = 64
                n_entries = v_sz // entry_sz
                for j in range(n_entries):
                    entry = vdata[j * entry_sz : (j + 1) * entry_sz]
                    crc = struct.unpack_from("<Q", entry, 0)[0]
                    sym_name = entry[8:].split(b"\x00")[0].decode("latin1")
                    symbol_crcs[sym_name] = f"0x{crc:08x}"

        return {
            "size": len(data),
            "vermagic": vermagic,
            "imported_symbols": imported,
            "symbol_crcs": symbol_crcs,
            "has_modversions": len(symbol_crcs) > 0,
            "sections": list(sections.keys())
        }

    @classmethod
    def verify_module(cls, ko_path: str, target_vermagic: str,
                       expected_crcs: Dict[str, str]) -> AbiVerificationResult:
        """
        Validates an LKM against expected vermagic and CRC requirements.
        """
        parsed = cls.parse_module(ko_path)
        if not parsed:
            res = AbiVerificationResult(module_path=ko_path)
            res.rejection_reasons.append(f"File not found: {ko_path}")
            return res

        res = AbiVerificationResult(
            module_path=ko_path,
            file_size_bytes=parsed.get("size", 0),
            vermagic=parsed.get("vermagic"),
            has_modversions=parsed.get("has_modversions", False),
            imported_symbols=parsed.get("imported_symbols", []),
            symbol_crcs=parsed.get("symbol_crcs", {})
        )

        # 1. Vermagic verification
        res.vermagic_matches = (res.vermagic == target_vermagic)
        if not res.vermagic_matches:
            res.rejection_reasons.append(
                f"Vermagic mismatch: found '{res.vermagic}', required '{target_vermagic}'"
            )

        # 2. Modversions check
        if not res.has_modversions:
            res.rejection_reasons.append(
                "Missing or empty __versions section; kernel CONFIG_MODVERSIONS=y will reject module"
            )

        # 3. CRC matching
        for sym, exp_crc in expected_crcs.items():
            if sym not in res.symbol_crcs:
                res.missing_crcs.append(sym)
                res.rejection_reasons.append(f"Missing required symbol CRC: {sym}")
            else:
                act_crc = res.symbol_crcs[sym]
                if act_crc.lower() == exp_crc.lower():
                    res.matched_crcs[sym] = act_crc
                else:
                    res.mismatched_crcs[sym] = (exp_crc, act_crc)
                    res.rejection_reasons.append(
                        f"CRC mismatch for {sym}: expected {exp_crc}, found {act_crc}"
                    )

        res.is_abi_compatible = (len(res.rejection_reasons) == 0)
        return res
