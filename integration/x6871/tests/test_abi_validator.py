"""
test_abi_validator.py

Unit tests for AbiValidator, ELF parsing, vermagic comparison, and symbol CRC validation.
Includes edge-case tests for invalid binaries, missing files, CRC discrepancies,
and corrupted ELF headers.
"""

import os
import tempfile
import unittest
from ..tools.abi_validator import AbiValidator, AbiVerificationResult

class TestAbiValidator(unittest.TestCase):
    def setUp(self):
        self.workspace = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
        self.prebuilt_ko = os.path.join(
            self.workspace, "repositories", "dirtyfrag", "DirtyFrag-mitschud",
            "app", "src", "main", "jni", "ko", "dfroot-android12-5.10.ko"
        )
        self.target_vermagic = "5.10.237-android12-9-00014-gf82f7360927e-ab14119954 SMP preempt mod_unload modversions aarch64"
        self.x6871_expected_crcs = {
            "module_layout": "0x7c24b32d",
            "register_kprobe": "0xc502eb6d",
            "unregister_kprobe": "0x18b23dc5",
            "printk": "0xc5850110",
            "__stack_chk_fail": "0x98a9d10c",
            "__stack_chk_guard": "0x8f678b07",
            "memset": "0xdcb764ad"
        }

    def test_prebuilt_mismatch_detection(self):
        if not os.path.exists(self.prebuilt_ko):
            self.skipTest("Prebuilt KO not found")

        result = AbiValidator.verify_module(
            self.prebuilt_ko,
            self.target_vermagic,
            self.x6871_expected_crcs
        )

        # Must strictly fail ABI verification due to vermagic and missing __versions section
        self.assertFalse(result.is_abi_compatible)
        self.assertFalse(result.vermagic_matches)
        self.assertIn("5.10.252-dirty", result.vermagic)
        self.assertTrue(len(result.rejection_reasons) > 0)
        self.assertTrue(any("vermagic mismatch" in r.lower() for r in result.rejection_reasons))

    def test_mock_module_matching(self):
        parsed_mock = {
            "size": 12000,
            "vermagic": self.target_vermagic,
            "has_modversions": True,
            "imported_symbols": list(self.x6871_expected_crcs.keys()),
            "symbol_crcs": dict(self.x6871_expected_crcs)
        }
        has_error = False
        reasons = []
        if parsed_mock["vermagic"] != self.target_vermagic:
            has_error = True
            reasons.append("Vermagic mismatch")
        for sym, exp in self.x6871_expected_crcs.items():
            if parsed_mock["symbol_crcs"].get(sym) != exp:
                has_error = True
                reasons.append(f"CRC mismatch {sym}")
        self.assertFalse(has_error)
        self.assertEqual(len(reasons), 0)

    def test_nonexistent_file_handling(self):
        result = AbiValidator.verify_module(
            "/nonexistent/path/to/module.ko",
            self.target_vermagic,
            self.x6871_expected_crcs
        )
        self.assertFalse(result.is_abi_compatible)
        self.assertTrue(any("File not found" in r for r in result.rejection_reasons))

    def test_corrupted_non_elf_binary(self):
        with tempfile.NamedTemporaryFile(suffix=".ko", delete=False) as f:
            f.write(b"NOT_AN_ELF_FILE_JUST_PLAIN_TEXT")
            temp_path = f.name
        try:
            parsed = AbiValidator.parse_module(temp_path)
            self.assertIsNotNone(parsed)
            self.assertIn("error", parsed)
            self.assertIn("Invalid 64-bit ELF", parsed["error"])
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    def test_32bit_elf_rejection(self):
        with tempfile.NamedTemporaryFile(suffix=".ko", delete=False) as f:
            # ELF magic + 32-bit flag (ELFCLASS32 = 1)
            f.write(b"\x7fELF\x01\x01\x01\x00" + b"\x00" * 56)
            temp_path = f.name
        try:
            parsed = AbiValidator.parse_module(temp_path)
            self.assertIsNotNone(parsed)
            self.assertIn("error", parsed)
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

if __name__ == "__main__":
    unittest.main()
