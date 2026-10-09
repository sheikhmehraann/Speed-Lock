"""
test_capability_engine.py

Unit tests for CapabilityEngine and backend evaluation.
"""

import os
import unittest
from ..core.capability_engine import CapabilityEngine
from ..core.device_identifier import DeviceIdentifier, DeviceProfile
from ..core.backend_interface import BackendState
from ..backends.backend_registry import BackendRegistry

class TestCapabilityEngine(unittest.TestCase):
    def setUp(self):
        self.workspace = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
        self.manifest_path = os.path.join(self.workspace, "integration", "x6871", "manifest", "device_manifest.json")
        self.profile = DeviceIdentifier.from_manifest(self.manifest_path)
        # Add config flags matching X6871
        self.profile.config_flags = {
            "CONFIG_XFRM": "y",
            "CONFIG_INET_ESP": "y",
            "CONFIG_MODULES": "y",
            "CONFIG_KPROBES": "y",
            "CONFIG_FUTEX": "y",
            "CONFIG_MODULE_SIG": "is not set"
        }
        self.profile.candidate_libraries = ["/vendor/lib64/libbinderdebug.so"]
        self.profile.has_insmod_symlink = True

        self.registry = BackendRegistry()
        self.engine = CapabilityEngine()
        for b in self.registry.list_backends():
            self.engine.register_backend(b)

    def test_evaluate_dfroot_metadata_compatible(self):
        results = self.engine.evaluate_all(self.profile)
        self.assertIn("dfroot", results)
        df_status = results["dfroot"]
        self.assertEqual(df_status.state, BackendState.METADATA_COMPATIBLE)
        self.assertTrue(df_status.prerequisites_met)
        # Check that it is NOT marked actionable until ABI verification passes
        self.assertFalse(df_status.is_actionable())
        self.assertTrue(any("mismatched" in b.lower() for b in df_status.critical_blockers))

    def test_evaluate_ghostlock_metadata_compatible(self):
        results = self.engine.evaluate_all(self.profile)
        self.assertIn("ghostlock", results)
        gl_status = results["ghostlock"]
        self.assertEqual(gl_status.state, BackendState.METADATA_COMPATIBLE)
        self.assertTrue(gl_status.prerequisites_met)
        self.assertFalse(gl_status.is_actionable())
        self.assertTrue(any("target profile" in b.lower() for b in gl_status.critical_blockers))

    def test_unsupported_on_missing_config(self):
        broken_profile = DeviceProfile(architecture="arm64")
        broken_profile.config_flags = {"CONFIG_XFRM": "is not set"}
        results = self.engine.evaluate_all(broken_profile)
        self.assertEqual(results["dfroot"].state, BackendState.UNSUPPORTED)
        self.assertFalse(results["dfroot"].prerequisites_met)

if __name__ == "__main__":
    unittest.main()
