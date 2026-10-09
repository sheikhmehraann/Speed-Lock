"""
test_regression_profiles.py

Regression test suite evaluating CapabilityEngine across a diverse matrix
of device architecture, kernel, and subsystem configurations.
Verifies that compatibility evaluations never yield false positives on incompatible
hardware, non-GKI kernels, or missing subsystem dependencies.
"""

import unittest
from ..core.device_identifier import DeviceProfile
from ..core.capability_engine import CapabilityEngine
from ..core.backend_interface import BackendState
from ..backends.dfroot_backend import DFRootBackend
from ..backends.ghostlock_backend import GhostLockBackend

class TestRegressionProfiles(unittest.TestCase):
    def setUp(self):
        self.engine = CapabilityEngine()
        self.dfroot = DFRootBackend()
        self.ghostlock = GhostLockBackend()
        self.engine.register_backend(self.dfroot)
        self.engine.register_backend(self.ghostlock)

    def test_x6871_stock_profile(self):
        """Infinix GT 20 Pro X6871: all kernel metadata prerequisites met."""
        prof = DeviceProfile(
            brand="Infinix",
            model="X6871",
            soc="MediaTek Dimensity 8200 Ultimate (MT6896)",
            architecture="arm64",
            page_size_bytes=4096,
            va_bits=39,
            kernel_release="5.10.237-android12-9-00014-gf82f7360927e-ab14119954",
            vermagic="5.10.237-android12-9-00014-gf82f7360927e-ab14119954 SMP preempt mod_unload modversions aarch64",
            candidate_libraries=["/vendor/lib64/libbinderdebug.so"],
            has_insmod_symlink=True,
            has_super_partition=True,
            config_flags={
                "CONFIG_XFRM": "y",
                "CONFIG_INET_ESP": "y",
                "CONFIG_MODULES": "y",
                "CONFIG_KPROBES": "y",
                "CONFIG_MODULE_SIG": "is not set",
                "CONFIG_FUTEX": "y"
            }
        )
        res = self.engine.evaluate_all(prof)
        self.assertEqual(res["dfroot"].state, BackendState.METADATA_COMPATIBLE)
        self.assertEqual(res["ghostlock"].state, BackendState.METADATA_COMPATIBLE)
        self.assertTrue(res["dfroot"].prerequisites_met)
        self.assertTrue(res["ghostlock"].prerequisites_met)

    def test_pixel_7_missing_vendor_lib(self):
        """Pixel 7 GKI 5.10 without /vendor/lib64/libbinderdebug.so -> DFRoot must be UNSUPPORTED."""
        prof = DeviceProfile(
            brand="Google",
            model="Pixel 7",
            soc="Tensor G2",
            architecture="arm64",
            page_size_bytes=4096,
            va_bits=39,
            kernel_release="5.10.157-android12-9-00021-g12345678",
            candidate_libraries=[], # No vendor candidate library
            has_insmod_symlink=True,
            config_flags={
                "CONFIG_XFRM": "y",
                "CONFIG_INET_ESP": "y",
                "CONFIG_MODULES": "y",
                "CONFIG_KPROBES": "y",
                "CONFIG_FUTEX": "y"
            }
        )
        res = self.engine.evaluate_all(prof)
        self.assertEqual(res["dfroot"].state, BackendState.UNSUPPORTED)
        self.assertFalse(res["dfroot"].prerequisites_met)
        self.assertIn("No known vendor target library", res["dfroot"].critical_blockers[0])
        # GhostLock should still be METADATA_COMPATIBLE because it does not require binderdebug
        self.assertEqual(res["ghostlock"].state, BackendState.METADATA_COMPATIBLE)

    def test_fire_max_11_disabled_modules(self):
        """Amazon Fire Max 11 with CONFIG_MODULES disabled -> DFRoot must be UNSUPPORTED."""
        prof = DeviceProfile(
            brand="Amazon",
            model="Fire Max 11",
            soc="MT8188J",
            architecture="arm64",
            page_size_bytes=4096,
            va_bits=39,
            kernel_release="5.10.101-android12-9-fireos",
            candidate_libraries=["/vendor/lib64/libbinderdebug.so"],
            has_insmod_symlink=True,
            config_flags={
                "CONFIG_XFRM": "y",
                "CONFIG_INET_ESP": "y",
                "CONFIG_MODULES": "is not set", # Disabled!
                "CONFIG_KPROBES": "y",
                "CONFIG_FUTEX": "y"
            }
        )
        res = self.engine.evaluate_all(prof)
        self.assertEqual(res["dfroot"].state, BackendState.UNSUPPORTED)
        self.assertTrue(any("CONFIG_MODULES" in b for b in res["dfroot"].critical_blockers))

    def test_legacy_32bit_arm_device(self):
        """32-bit ARM (arm) architecture -> both 64-bit backends must be UNSUPPORTED."""
        prof = DeviceProfile(
            brand="Legacy",
            model="Device32",
            soc="OldSoC",
            architecture="arm", # 32-bit ARM
            page_size_bytes=4096,
            va_bits=32,
            kernel_release="4.19.110",
            config_flags={
                "CONFIG_XFRM": "y",
                "CONFIG_INET_ESP": "y",
                "CONFIG_MODULES": "y",
                "CONFIG_KPROBES": "y",
                "CONFIG_FUTEX": "y"
            }
        )
        res = self.engine.evaluate_all(prof)
        self.assertEqual(res["dfroot"].state, BackendState.UNSUPPORTED)
        self.assertEqual(res["ghostlock"].state, BackendState.UNSUPPORTED)
        self.assertIn("arm64", res["dfroot"].critical_blockers[0])
        self.assertIn("arm", res["ghostlock"].critical_blockers[0])

    def test_missing_futex_subsystem(self):
        """Kernel compiled without CONFIG_FUTEX -> GhostLock must be UNSUPPORTED."""
        prof = DeviceProfile(
            brand="Generic",
            model="CustomKernel",
            architecture="arm64",
            page_size_bytes=4096,
            va_bits=39,
            kernel_release="5.10.237-android12-9",
            config_flags={
                "CONFIG_XFRM": "y",
                "CONFIG_INET_ESP": "y",
                "CONFIG_MODULES": "y",
                "CONFIG_KPROBES": "y",
                "CONFIG_FUTEX": "is not set" # Futex disabled
            },
            candidate_libraries=["/vendor/lib64/libbinderdebug.so"],
            has_insmod_symlink=True
        )
        res = self.engine.evaluate_all(prof)
        self.assertEqual(res["ghostlock"].state, BackendState.UNSUPPORTED)
        self.assertIn("CONFIG_FUTEX", res["ghostlock"].critical_blockers[0])

if __name__ == "__main__":
    unittest.main()
