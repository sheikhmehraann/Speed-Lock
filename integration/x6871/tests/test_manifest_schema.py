"""
test_manifest_schema.py

Schema validator test suite for device manifests.
Validates production manifests against manifest_schema.json constraints
and verifies rejection of invalid schemas, malformed types, and unlisted enum states.
"""

import os
import json
import unittest
from typing import Dict, Any, List

class ManifestSchemaValidator:
    """Lightweight standalone schema validator against manifest_schema.json specifications."""

    ALLOWED_ARCHITECTURES = {"arm64", "arm", "x86_64", "x86"}
    ALLOWED_STATES = {
        "UNSUPPORTED",
        "UNTESTED",
        "METADATA_COMPATIBLE",
        "ABI_COMPATIBLE",
        "VERIFIED_CONFIRMED"
    }

    @classmethod
    def validate(cls, manifest: Dict[str, Any]) -> List[str]:
        errors = []

        # Top-level required
        for req in ["device_info", "kernel_info", "security_profile", "backend_capabilities"]:
            if req not in manifest:
                errors.append(f"Missing top-level required section: {req}")

        if errors:
            return errors

        # device_info validation
        d_info = manifest.get("device_info", {})
        if not isinstance(d_info, dict):
            errors.append("device_info must be an object")
        else:
            for field in ["brand", "model", "marketing_name", "soc", "board"]:
                if field not in d_info:
                    errors.append(f"Missing device_info required field: {field}")
                elif not isinstance(d_info[field], str):
                    errors.append(f"device_info.{field} must be a string")

        # kernel_info validation
        k_info = manifest.get("kernel_info", {})
        if not isinstance(k_info, dict):
            errors.append("kernel_info must be an object")
        else:
            for field in ["release_string", "vermagic", "kmi_generation"]:
                if field not in k_info:
                    errors.append(f"Missing kernel_info required field: {field}")
                elif not isinstance(k_info[field], str):
                    errors.append(f"kernel_info.{field} must be a string")

            arch = k_info.get("architecture")
            if arch not in cls.ALLOWED_ARCHITECTURES:
                errors.append(f"Invalid kernel_info.architecture: {arch}")

            ps = k_info.get("page_size_bytes")
            if not isinstance(ps, int) or ps <= 0:
                errors.append(f"kernel_info.page_size_bytes must be a positive integer, got {ps}")

            va = k_info.get("va_bits")
            if not isinstance(va, int) or va <= 0:
                errors.append(f"kernel_info.va_bits must be a positive integer, got {va}")

        # security_profile validation
        s_prof = manifest.get("security_profile", {})
        if not isinstance(s_prof, dict):
            errors.append("security_profile must be an object")
        else:
            if "avb_security_patch" not in s_prof or not isinstance(s_prof["avb_security_patch"], str):
                errors.append("security_profile.avb_security_patch must be a string")
            for bfield in ["module_sig_enforced", "cfi_enabled", "scs_enabled", "kaslr_enabled"]:
                if bfield not in s_prof or not isinstance(s_prof[bfield], bool):
                    errors.append(f"security_profile.{bfield} must be a boolean")

        # backend_capabilities validation
        b_caps = manifest.get("backend_capabilities", {})
        if not isinstance(b_caps, dict) or len(b_caps) == 0:
            errors.append("backend_capabilities must be a non-empty object")
        else:
            for b_id, entry in b_caps.items():
                if not isinstance(entry, dict):
                    errors.append(f"backend_capabilities.{b_id} must be an object")
                    continue
                for bfield in ["backend_name", "state", "reasons", "prerequisites_met"]:
                    if bfield not in entry:
                        errors.append(f"Missing backend_capabilities.{b_id}.{bfield}")

                st = entry.get("state")
                if st not in cls.ALLOWED_STATES:
                    errors.append(f"Invalid backend state for {b_id}: '{st}'")

                if not isinstance(entry.get("reasons"), list):
                    errors.append(f"backend_capabilities.{b_id}.reasons must be an array")

                if not isinstance(entry.get("prerequisites_met"), bool):
                    errors.append(f"backend_capabilities.{b_id}.prerequisites_met must be a boolean")

        return errors

class TestManifestSchema(unittest.TestCase):
    def setUp(self):
        self.workspace = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
        self.manifest_path = os.path.join(self.workspace, "integration", "x6871", "manifest", "device_manifest.json")
        self.schema_path = os.path.join(self.workspace, "integration", "x6871", "manifest", "manifest_schema.json")

    def test_production_manifest_is_valid(self):
        with open(self.manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
        errors = ManifestSchemaValidator.validate(manifest)
        self.assertEqual(errors, [], f"Production manifest validation failed: {errors}")

    def test_schema_file_exists_and_parses(self):
        self.assertTrue(os.path.exists(self.schema_path))
        with open(self.schema_path, "r", encoding="utf-8") as f:
            schema = json.load(f)
        self.assertEqual(schema.get("title"), "SpeedLockDeviceManifest")
        self.assertIn("device_info", schema.get("required", []))

    def test_rejects_missing_required_top_level(self):
        invalid = {"device_info": {}}
        errors = ManifestSchemaValidator.validate(invalid)
        self.assertTrue(any("kernel_info" in e for e in errors))
        self.assertTrue(any("security_profile" in e for e in errors))

    def test_rejects_invalid_architecture(self):
        with open(self.manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
        manifest["kernel_info"]["architecture"] = "mips"
        errors = ManifestSchemaValidator.validate(manifest)
        self.assertTrue(any("Invalid kernel_info.architecture" in e for e in errors))

    def test_rejects_invalid_backend_state(self):
        with open(self.manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
        manifest["backend_capabilities"]["dfroot"]["state"] = "SUPER_ROOTED_100%"
        errors = ManifestSchemaValidator.validate(manifest)
        self.assertTrue(any("Invalid backend state" in e for e in errors))

    def test_rejects_non_integer_page_size(self):
        with open(self.manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
        manifest["kernel_info"]["page_size_bytes"] = "4096" # String instead of int
        errors = ManifestSchemaValidator.validate(manifest)
        self.assertTrue(any("page_size_bytes must be a positive integer" in e for e in errors))

    def test_bottom_navigation_menu_limit(self):
        import xml.etree.ElementTree as ET
        menu_path = os.path.join(self.workspace, "speedlock-app", "app", "src", "main", "res", "menu", "bottom_nav_menu.xml")
        self.assertTrue(os.path.exists(menu_path), f"bottom_nav_menu.xml not found at {menu_path}")
        tree = ET.parse(menu_path)
        root = tree.getroot()
        items = root.findall(".//item")
        self.assertLessEqual(len(items), 5,
            f"BottomNavigationView supports maximum 5 items (found {len(items)}). Exceeding causes runtime InflateException!")
        self.assertEqual(len(items), 5, "Expected exactly 5 items in bottom_nav_menu.xml")

if __name__ == "__main__":
    unittest.main()
