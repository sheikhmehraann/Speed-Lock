"""
ghostlock_backend.py

Backend adapter for GhostLock / IonStack (CVE-2026-43499).
Evaluates futex subsystem configuration, memory layout, and target profile availability.
"""

import os
from typing import List, Dict, Any
from ..core.backend_interface import IExploitBackend, BackendStatus, BackendState
from ..core.device_identifier import DeviceProfile

class GhostLockBackend(IExploitBackend):
    @property
    def backend_id(self) -> str:
        return "ghostlock"

    @property
    def display_name(self) -> str:
        return "GhostLock / IonStack (CVE-2026-43499)"

    @property
    def vulnerability_cve(self) -> str:
        return "CVE-2026-43499"

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
            status.critical_blockers.append(f"Architecture '{profile.architecture}' unsupported")
            return status

        cfg = profile.config_flags
        if cfg.get("CONFIG_FUTEX") != "y":
            status.state = BackendState.UNSUPPORTED
            status.critical_blockers.append("CONFIG_FUTEX is not enabled in kernel")
            return status

        status.reasons.append("Futex PI subsystem is compiled into kernel (CONFIG_FUTEX=y)")

        if profile.page_size_bytes == 4096:
            status.reasons.append("4KB page size compatible with standard ARM64 IonStack memory spray")
        else:
            status.critical_blockers.append(f"Non-standard page size: {profile.page_size_bytes} bytes")

        if profile.va_bits == 39:
            status.reasons.append("39-bit Virtual Address space matches Galaxy S22+ (IonStack-S22U) layout")
        else:
            status.reasons.append(f"Virtual address space is {profile.va_bits} bits")

        status.prerequisites_met = True
        status.state = BackendState.METADATA_COMPATIBLE
        status.critical_blockers.append(
            "Missing compiled target profile (target.h / ionstack.conf) containing exact task_struct, "
            "cred, and call_usermodehelper_exec_work offsets for build ab14119954"
        )
        return status

    def verify_artifacts(self, workspace_root: str) -> Dict[str, bool]:
        ghostlock_dir = os.path.join(workspace_root, "repositories", "ghostlock-ionstack", "GhostLock")
        app_dir = os.path.join(workspace_root, "repositories", "ghostlock-ionstack", "ghostlock-app")
        return {
            "ghostlock_source_present": os.path.exists(ghostlock_dir),
            "ghostlock_app_present": os.path.exists(app_dir),
            "rootchain_c_present": os.path.exists(os.path.join(ghostlock_dir, "src", "rootchain.c"))
        }
