"""
test_device_identifier.py

Unit tests for DeviceIdentifier and DeviceProfile.
Verifies parsing of manifests, firmware directories, build.prop files,
and dynamic detection of hardware and partition properties.
"""

import os
import json
import tempfile
import unittest
from ..core.device_identifier import DeviceIdentifier, DeviceProfile

class TestDeviceIdentifier(unittest.TestCase):
    def setUp(self):
        self.workspace = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
        self.manifest_path = os.path.join(self.workspace, "integration", "x6871", "manifest", "device_manifest.json")
        self.firmware_dir = os.path.join(self.workspace, "research", "firmware", "X6871")

    def test_from_manifest(self):
        profile = DeviceIdentifier.from_manifest(self.manifest_path)
        self.assertEqual(profile.brand, "Infinix")
        self.assertEqual(profile.model, "X6871")
        self.assertEqual(profile.page_size_bytes, 4096)
        self.assertEqual(profile.va_bits, 39)
        self.assertTrue(profile.is_gki_5_10())
        self.assertIn("5.10.237-android12-9", profile.kernel_release)
        self.assertIn("modversions aarch64", profile.vermagic)
        self.assertTrue(profile.has_insmod_symlink)
        self.assertTrue(profile.has_super_partition)

    def test_from_manifest_dynamic_flags(self):
        # Create a manifest without insmod or super partition to verify dynamic evaluation
        dummy_manifest = {
            "device_info": {
                "brand": "TestBrand",
                "model": "TestModel",
                "marketing_name": "TestMarketing",
                "soc": "TestSoC",
                "board": "TestBoard",
                "has_super_partition": False
            },
            "kernel_info": {
                "architecture": "arm64",
                "page_size_bytes": 4096,
                "va_bits": 39,
                "release_string": "5.10.100",
                "vermagic": "5.10.100 vermagic",
                "kmi_generation": "android12-9"
            },
            "security_profile": {
                "avb_security_patch": "2026-01-01",
                "insmod_available": False,
                "module_sig_enforced": False,
                "cfi_enabled": True,
                "scs_enabled": True,
                "kaslr_enabled": True
            },
            "backend_capabilities": {
                "dfroot": {
                    "backend_name": "DFRoot",
                    "state": "UNSUPPORTED",
                    "prerequisites_met": False,
                    "reasons": []
                }
            }
        }
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False, encoding="utf-8") as f:
            json.dump(dummy_manifest, f)
            tpath = f.name
        try:
            profile = DeviceIdentifier.from_manifest(tpath)
            self.assertFalse(profile.has_insmod_symlink)
            self.assertFalse(profile.has_super_partition)
        finally:
            if os.path.exists(tpath):
                os.remove(tpath)

    def test_parse_build_prop(self):
        prop_content = """
        # System properties
        ro.product.brand=Infinix
        ro.product.model=X6871
        ro.vendor.mediatek.platform=MT6895
        # Comment line
        ro.build.version.security_patch=2026-07-01
        """
        with tempfile.NamedTemporaryFile(mode="w", suffix=".prop", delete=False, encoding="utf-8") as f:
            f.write(prop_content)
            tpath = f.name
        try:
            props = DeviceIdentifier.parse_build_prop(tpath)
            self.assertEqual(props.get("ro.product.brand"), "Infinix")
            self.assertEqual(props.get("ro.product.model"), "X6871")
            self.assertEqual(props.get("ro.vendor.mediatek.platform"), "MT6895")
            self.assertEqual(props.get("ro.build.version.security_patch"), "2026-07-01")
        finally:
            if os.path.exists(tpath):
                os.remove(tpath)

    def test_from_firmware_directory(self):
        if not os.path.exists(self.firmware_dir):
            self.skipTest("Firmware directory not present")
        profile = DeviceIdentifier.from_firmware_directory(self.firmware_dir)
        self.assertEqual(profile.brand, "Infinix")
        self.assertEqual(profile.model, "X6871")
        self.assertEqual(profile.marketing_name, "Infinix GT 20 Pro")
        self.assertEqual(profile.platform, "MT6895")
        self.assertEqual(profile.soc, "MediaTek Dimensity 8200 Ultimate (MT6896/MT6895)")
        self.assertEqual(profile.build_display_id, "X6871-H962CF-U-BASE-260618V1066DevT")
        self.assertEqual(profile.avb_security_patch, "2026-07-01")
        self.assertGreater(len(profile.system_properties), 0)
        
        kernel_candidates = [
            os.path.join(self.firmware_dir, "kernel-metadata", "Image-x6871-5.10.237"),
            os.path.join(self.firmware_dir, "kernel-metadata", "Image")
        ]
        if any(os.path.exists(k) for k in kernel_candidates):
            self.assertIn("5.10.237", profile.kernel_release)
            self.assertIn("modversions aarch64", profile.vermagic)
            self.assertEqual(profile.va_bits, 39)
            
        self.assertEqual(profile.page_size_bytes, 4096)
        self.assertTrue(profile.has_insmod_symlink)
        self.assertTrue(profile.has_super_partition)
        self.assertIn("/vendor/lib64/libbinderdebug.so", profile.candidate_libraries)
        self.assertEqual(profile.config_flags.get("CONFIG_XFRM"), "y")
        self.assertEqual(profile.config_flags.get("CONFIG_MODULE_SIG"), "is not set")

if __name__ == "__main__":
    unittest.main()
