"""
device_identifier.py

Parses and validates device identity, SoC, and kernel firmware artifacts
from live system properties, saved firmware files, or JSON manifests.
"""

import os
import re
from dataclasses import dataclass, field
from typing import Dict, Any, Optional, List

@dataclass
class DeviceProfile:
    brand: str = "Unknown"
    model: str = "Unknown"
    marketing_name: str = "Unknown"
    soc: str = "Unknown"
    board: str = "Unknown"
    architecture: str = "arm64"
    page_size_bytes: int = 4096
    va_bits: int = 39
    kernel_release: str = ""
    vermagic: str = ""
    kmi_generation: str = ""
    avb_security_patch: str = ""
    build_fingerprint: str = ""
    build_display_id: str = ""
    platform: str = ""
    config_flags: Dict[str, str] = field(default_factory=dict)
    candidate_libraries: List[str] = field(default_factory=list)
    has_insmod_symlink: bool = False
    has_super_partition: bool = False
    system_properties: Dict[str, str] = field(default_factory=dict)

    def is_gki_5_10(self) -> bool:
        return self.kernel_release.startswith("5.10.") and "android" in self.kernel_release

class DeviceIdentifier:
    @staticmethod
    def parse_build_prop(prop_path: str) -> Dict[str, str]:
        """Parses an Android build.prop file into a key-value dictionary."""
        props: Dict[str, str] = {}
        if not os.path.exists(prop_path):
            return props
        with open(prop_path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" in line:
                    k, v = line.split("=", 1)
                    props[k.strip()] = v.strip()
        return props

    @staticmethod
    def from_manifest(manifest_path: str) -> DeviceProfile:
        import json
        if not os.path.exists(manifest_path):
            raise FileNotFoundError(f"Manifest not found: {manifest_path}")
        with open(manifest_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        d_info = data.get("device_info", {})
        k_info = data.get("kernel_info", {})
        s_prof = data.get("security_profile", {})
        b_caps = data.get("backend_capabilities", {})

        # Dynamically determine insmod symlink and super partition availability
        has_insmod = s_prof.get("insmod_available")
        if has_insmod is None:
            dfroot_reasons = b_caps.get("dfroot", {}).get("reasons", [])
            has_insmod = any("/vendor/bin/insmod" in r for r in dfroot_reasons)

        has_super = d_info.get("has_super_partition")
        if has_super is None:
            dfroot_reasons = b_caps.get("dfroot", {}).get("reasons", [])
            has_super = any("super" in r.lower() for r in dfroot_reasons)

        return DeviceProfile(
            brand=d_info.get("brand", "Unknown"),
            model=d_info.get("model", "Unknown"),
            marketing_name=d_info.get("marketing_name", "Unknown"),
            soc=d_info.get("soc", "Unknown"),
            board=d_info.get("board", "Unknown"),
            architecture=k_info.get("architecture", "arm64"),
            page_size_bytes=k_info.get("page_size_bytes", 4096),
            va_bits=k_info.get("va_bits", 39),
            kernel_release=k_info.get("release_string", ""),
            vermagic=k_info.get("vermagic", ""),
            kmi_generation=k_info.get("kmi_generation", ""),
            avb_security_patch=s_prof.get("avb_security_patch", ""),
            has_insmod_symlink=bool(has_insmod),
            has_super_partition=bool(has_super)
        )

    @staticmethod
    def from_firmware_directory(firmware_dir: str) -> DeviceProfile:
        profile = DeviceProfile()

        # 1. Parse build.prop files (vendor_ramdisk_build.prop, boot_ramdisk_build.prop)
        vendor_prop_path = os.path.join(firmware_dir, "kernel-metadata", "vendor_ramdisk_build.prop")
        boot_prop_path = os.path.join(firmware_dir, "kernel-metadata", "boot_ramdisk_build.prop")

        props: Dict[str, str] = {}
        if os.path.exists(boot_prop_path):
            props.update(DeviceIdentifier.parse_build_prop(boot_prop_path))
        if os.path.exists(vendor_prop_path):
            props.update(DeviceIdentifier.parse_build_prop(vendor_prop_path))

        profile.system_properties = props

        # Extract identity properties from parsed build.prop
        if props:
            # Brand identification
            for k in ["ro.product.vendor.brand", "ro.product.system.brand", "ro.product.odm.brand"]:
                val = props.get(k)
                if val and val.lower() != "alps":
                    profile.brand = val
                    break
            if profile.brand == "Unknown" and props.get("ro.product.system.brand"):
                profile.brand = props["ro.product.system.brand"]

            # Model identification
            for k in ["ro.product.vendor.model", "ro.product.system.model", "ro.product.odm.model"]:
                val = props.get(k)
                if val and not val.startswith("mgvi_64"):
                    # Normalize if brand prefix is attached (e.g., 'Infinix X6871' -> 'X6871')
                    if val.startswith(profile.brand + " "):
                        profile.model = val[len(profile.brand) + 1:]
                    else:
                        profile.model = val
                    break

            # Platform and SoC
            profile.platform = props.get("ro.vendor.mediatek.platform", "")
            if profile.platform == "MT6895" or "MT6895" in props.get("ro.gfx.driver.0", ""):
                profile.soc = "MediaTek Dimensity 8200 Ultimate (MT6896/MT6895)"
            elif profile.platform:
                profile.soc = f"MediaTek {profile.platform}"

            # Board / product name
            profile.board = props.get("ro.build.product") or props.get("ro.product.board") or "Unknown"

            # Marketing name
            if profile.brand == "Infinix" and "X6871" in profile.model:
                profile.marketing_name = "Infinix GT 20 Pro"

            # Build metadata
            profile.build_fingerprint = (
                props.get("ro.vendor.build.fingerprint")
                or props.get("ro.system.build.fingerprint")
                or props.get("ro.bootimage.build.fingerprint", "")
            )
            profile.build_display_id = (
                props.get("ro.build.display.id")
                or props.get("ro.vendor.tran.version.release", "")
            )
            profile.avb_security_patch = props.get("ro.build.version.security_patch", "")

        # 2. Inspect kernel image (Image-x6871-5.10.237 or any Image in kernel-metadata)
        kernel_candidates = [
            os.path.join(firmware_dir, "kernel-metadata", "Image-x6871-5.10.237"),
            os.path.join(firmware_dir, "kernel-metadata", "Image")
        ]
        kernel_path = next((k for k in kernel_candidates if os.path.exists(k)), None)

        if kernel_path and os.path.exists(kernel_path):
            with open(kernel_path, "rb") as f:
                kdata = f.read()

            m_rel = re.search(rb"Linux version ([0-9]+\.[0-9]+\.[0-9]+-android[0-9]+-[^\s]+)", kdata)
            if m_rel:
                profile.kernel_release = m_rel.group(1).decode("latin1")

            m_vm = re.search(rb"(5\.10\.[0-9]+-android[0-9]+-[0-9]+-[^\x00]+modversions aarch64)", kdata)
            if m_vm:
                profile.vermagic = m_vm.group(1).decode("latin1")

            m_kmi = re.search(r"android\d+-\d+", profile.kernel_release)
            if m_kmi:
                profile.kmi_generation = m_kmi.group(0)

        # 3. Inspect kernel config
        config_path = os.path.join(firmware_dir, "kernel-metadata", "extracted_config.txt")
        if os.path.exists(config_path):
            with open(config_path, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    if line.startswith("# CONFIG_") and "is not set" in line:
                        k = line.split()[1]
                        profile.config_flags[k] = "is not set"
                    elif line.startswith("CONFIG_") and "=" in line:
                        k, v = line.split("=", 1)
                        profile.config_flags[k] = v

            # Infer page size and VA bits
            if profile.config_flags.get("CONFIG_ARM64_4K_PAGES") == "y":
                profile.page_size_bytes = 4096
            elif profile.config_flags.get("CONFIG_ARM64_16K_PAGES") == "y":
                profile.page_size_bytes = 16384

            if "CONFIG_ARM64_VA_BITS" in profile.config_flags:
                try:
                    profile.va_bits = int(profile.config_flags["CONFIG_ARM64_VA_BITS"])
                except ValueError:
                    pass

        # 4. Check vendor candidate libraries
        vendor_map = os.path.join(firmware_dir, "stock-builds", "vendor.map")
        if os.path.exists(vendor_map):
            with open(vendor_map, "r", encoding="utf-8", errors="ignore") as f:
                vtext = f.read()
            for cand in ["/vendor/lib64/libbinderdebug.so", "/vendor/lib64/libstagefrighthw.so"]:
                if cand in vtext:
                    profile.candidate_libraries.append(cand)

        # 5. Check insmod symlink
        installed_vendor = os.path.join(firmware_dir, "kernel-metadata", "installed-files-vendor.txt")
        if os.path.exists(installed_vendor):
            with open(installed_vendor, "r", encoding="utf-8", errors="ignore") as f:
                if "/vendor/bin/insmod" in f.read():
                    profile.has_insmod_symlink = True

        # 6. Check scatter file for partitions and SoC fallback
        scatter_files = [
            os.path.join(firmware_dir, "partition-metadata", "MT6895_Android_scatter.xml"),
            os.path.join(firmware_dir, "partition-metadata", "MT6895_Android_scatter_untested.xml")
        ]
        for scatter in scatter_files:
            if os.path.exists(scatter):
                with open(scatter, "r", encoding="utf-8", errors="ignore") as f:
                    stext = f.read()
                    profile.has_super_partition = "super" in stext
                    if profile.brand == "Unknown" and "Infinix" in stext:
                        profile.brand = "Infinix"
                    if profile.model == "Unknown" and "X6871" in stext:
                        profile.model = "X6871"
                    if profile.soc == "Unknown" and "MT6895" in stext:
                        profile.soc = "MediaTek Dimensity 8200 Ultimate (MT6896)"
                break

        return profile
