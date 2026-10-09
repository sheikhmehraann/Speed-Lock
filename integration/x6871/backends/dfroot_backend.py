"""
dfroot_backend.py

Backend adapter for DFRoot (CVE-2026-43284, DirtyFrag).
Encapsulates DFRoot-specific prerequisite evaluation, LKM ABI validation,
and failure-reason diagnosis.
"""

import os
from typing import List, Dict, Any
from ..core.backend_interface import IExploitBackend, BackendStatus, BackendState
from ..core.device_identifier import DeviceProfile
from ..tools.abi_validator import AbiValidator

class DFRootBackend(IExploitBackend):
    @property
    def backend_id(self) -> str:
        return "dfroot"

    @property
    def display_name(self) -> str:
        return "DFRoot (CVE-2026-43284)"

    @property
    def vulnerability_cve(self) -> str:
        return "CVE-2026-43284"

    @property
    def supported_architectures(self) -> List[str]:
        return ["arm64"]

    def evaluate_compatibility(self, profile: DeviceProfile) -> BackendStatus:
        status = BackendStatus(
            backend_id=self.backend_id,
            display_name=self.display_name,
            state=BackendState.UNTESTED,
            prerequisites_met=False
        )

        if profile.architecture not in self.supported_architectures:
            status.state = BackendState.UNSUPPORTED
            status.critical_blockers.append(f"Architecture '{profile.architecture}' unsupported (requires arm64)")
            return status

        # Check required .config flags
        cfg = profile.config_flags
        prereq_checks = {
            "CONFIG_XFRM": ("=y", "IPsec transform subsystem"),
            "CONFIG_INET_ESP": ("=y", "IPsec ESP protocol support"),
            "CONFIG_MODULES": ("=y", "Loadable kernel module support"),
            "CONFIG_KPROBES": ("=y", "Kprobes for dynamic symbol resolution")
        }

        missing_configs = []
        for opt, (expected, desc) in prereq_checks.items():
            val = cfg.get(opt)
            if val != "y":
                missing_configs.append(f"{opt} (expected {expected}, found {val}; needed for {desc})")

        if missing_configs:
            status.state = BackendState.UNSUPPORTED
            status.critical_blockers.extend(missing_configs)
            return status

        status.reasons.append("Kernel configuration satisfies all required DirtyFrag subsystems (XFRM, ESP, KPROBES, MODULES)")

        # Module signature check
        if cfg.get("CONFIG_MODULE_SIG") == "y":
            status.reasons.append("WARNING: CONFIG_MODULE_SIG=y is enabled; LKM requires cryptographic vendor signature")
        else:
            status.reasons.append("Module signing disabled (# CONFIG_MODULE_SIG is not set); allows unsigned LKM loading")

        # Filesystem candidate check
        if profile.candidate_libraries:
            status.reasons.append(f"Candidate library present: {profile.candidate_libraries[0]}")
        else:
            status.critical_blockers.append("No known vendor target library (/vendor/lib64/libbinderdebug.so) found in firmware")

        # Insmod check
        if profile.has_insmod_symlink:
            status.reasons.append("Executable /vendor/bin/insmod exists")
        else:
            status.critical_blockers.append("Executable /vendor/bin/insmod missing from vendor filesystem")

        # Evaluate state based on blockers
        if status.critical_blockers:
            status.state = BackendState.UNSUPPORTED
            status.prerequisites_met = False
        else:
            # All metadata prerequisites are met!
            status.prerequisites_met = True
            status.state = BackendState.METADATA_COMPATIBLE
            # Now note the ABI blocker
            status.critical_blockers.append(
                "Mismatched LKM vermagic in circulating prebuilts ('5.10.252-dirty'); "
                f"target kernel requires matching vermagic ('{profile.vermagic}') and symbol CRCs"
            )

        return status

    def verify_artifacts(self, workspace_root: str) -> Dict[str, bool]:
        dfroot_dir = os.path.join(workspace_root, "repositories", "dirtyfrag", "DFRoot")
        checks = {
            "dfroot_source_present": os.path.exists(dfroot_dir),
            "exp_c_present": os.path.exists(os.path.join(dfroot_dir, "app", "src", "main", "jni", "exp.c")),
            "splicehelper_c_present": os.path.exists(os.path.join(dfroot_dir, "app", "src", "main", "jni", "splicehelper.c")),
            "libcxx_s_present": os.path.exists(os.path.join(dfroot_dir, "app", "src", "main", "jni", "libcxx.S")),
            "lkm_source_present": os.path.exists(os.path.join(dfroot_dir, "lkm", "dfroot.c"))
        }
        return checks
