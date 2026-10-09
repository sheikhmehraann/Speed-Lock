"""
test_build_validator.py

Unit tests for BuildValidator, checking toolchain detection,
cross-project repository file auditing, and Kbuild readiness reporting.
"""

import os
import unittest
from ..tools.build_validator import BuildValidator, BuildValidationReport

class TestBuildValidator(unittest.TestCase):
    def setUp(self):
        self.workspace = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
        self.validator = BuildValidator(self.workspace)

    def test_toolchain_detection(self):
        tools = self.validator.check_toolchains()
        self.assertIn("javac", tools)
        self.assertIn("gcc", tools)
        self.assertIn("clang", tools)
        self.assertIn("make", tools)
        # Verify ToolchainStatus attributes
        for name, status in tools.items():
            self.assertEqual(status.tool, name)
            self.assertIsInstance(status.available, bool)

    def test_repository_audit(self):
        audits = self.validator.audit_repositories()
        self.assertIn("DFRoot", audits)
        self.assertIn("DirtyInit", audits)
        self.assertIn("GhostLock", audits)
        self.assertIn("ghostlock-app", audits)
        self.assertIn("UniRoot", audits)

        # In this workspace, all 5 repositories should be present with all required files
        for name, data in audits.items():
            self.assertTrue(data["directory_exists"], f"Repo directory missing for {name}")
            self.assertTrue(data["all_files_present"], f"Missing required files in {name}: {data['missing_files']}")
            self.assertEqual(data["missing_files"], [])

    def test_kbuild_readiness(self):
        kbuild = self.validator.check_kbuild_readiness()
        self.assertIn("KDIR_set", kbuild)
        self.assertIn("ANDROID_NDK_set", kbuild)

    def test_validate_all_verdict(self):
        report = self.validator.validate_all()
        self.assertIsInstance(report, BuildValidationReport)
        self.assertIn(report.summary_verdict, ["READY", "SOURCE_AUDITED_TOOLCHAIN_MISSING", "BLOCKED"])
        # If NDK or KDIR are not configured in environment, verdict must be SOURCE_AUDITED_TOOLCHAIN_MISSING
        if not report.can_build_lkm or not report.can_build_native_jni:
            self.assertEqual(report.summary_verdict, "SOURCE_AUDITED_TOOLCHAIN_MISSING")
            self.assertTrue(len(report.blockers) > 0)

if __name__ == "__main__":
    unittest.main()
