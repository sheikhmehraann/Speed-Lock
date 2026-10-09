"""
test_backend_registry.py

Unit tests for BackendRegistry.
"""

import unittest
from ..backends.backend_registry import BackendRegistry
from ..backends.dfroot_backend import DFRootBackend
from ..backends.ghostlock_backend import GhostLockBackend

class TestBackendRegistry(unittest.TestCase):
    def setUp(self):
        self.registry = BackendRegistry()

    def test_default_backends_registered(self):
        backends = self.registry.list_backends()
        self.assertGreaterEqual(len(backends), 2)
        ids = [b.backend_id for b in backends]
        self.assertIn("dfroot", ids)
        self.assertIn("ghostlock", ids)

    def test_get_by_id(self):
        df = self.registry.get("dfroot")
        self.assertIsNotNone(df)
        self.assertEqual(df.backend_id, "dfroot")
        self.assertEqual(df.vulnerability_cve, "CVE-2026-43284")

    def test_get_by_cve(self):
        cve_backends = self.registry.get_by_cve("cve-2026-43284")
        self.assertEqual(len(cve_backends), 1)
        self.assertEqual(cve_backends[0].backend_id, "dfroot")

if __name__ == "__main__":
    unittest.main()
