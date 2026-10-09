"""
capability_engine.py

Orchestrates capability evaluations across registered exploit backends
and computes explicit lifecycle states for a given DeviceProfile.
"""

from typing import Dict, List, Any
from .backend_interface import IExploitBackend, BackendStatus, BackendState
from .device_identifier import DeviceProfile
from .diagnostic_logger import DiagnosticLogger, FindingClassification

class CapabilityEngine:
    def __init__(self, logger: DiagnosticLogger = None):
        self.backends: Dict[str, IExploitBackend] = {}
        self.logger = logger or DiagnosticLogger("CapabilityEngine")

    def register_backend(self, backend: IExploitBackend) -> None:
        self.backends[backend.backend_id] = backend

    def evaluate_all(self, profile: DeviceProfile) -> Dict[str, BackendStatus]:
        results = {}
        for b_id, backend in self.backends.items():
            status = backend.evaluate_compatibility(profile)
            results[b_id] = status

            # Epistemological logging
            if status.state == BackendState.VERIFIED_CONFIRMED:
                classification = FindingClassification.VERIFIED
            elif status.state == BackendState.ABI_COMPATIBLE:
                classification = FindingClassification.STRONGLY_INDICATED
            elif status.state == BackendState.METADATA_COMPATIBLE:
                classification = FindingClassification.STRONGLY_INDICATED
            elif status.state == BackendState.UNTESTED:
                classification = FindingClassification.UNVERIFIED
            else:
                classification = FindingClassification.DISPROVED

            self.logger.log_finding(
                topic=f"BackendCapability:{backend.backend_id}",
                classification=classification,
                summary=f"{backend.display_name} -> {status.state.value}",
                evidence_source="CapabilityEngine.evaluate_all()",
                details="; ".join(status.reasons + [f"BLOCKER: {b}" for b in status.critical_blockers])
            )
        return results

    def generate_summary(self, profile: DeviceProfile, results: Dict[str, BackendStatus]) -> Dict[str, Any]:
        return {
            "device": f"{profile.brand} {profile.model} ({profile.marketing_name})",
            "soc": profile.soc,
            "kernel_release": profile.kernel_release,
            "vermagic": profile.vermagic,
            "page_size": profile.page_size_bytes,
            "va_bits": profile.va_bits,
            "backends": {
                b_id: {
                    "name": status.display_name,
                    "state": status.state.value,
                    "is_actionable": status.is_actionable(),
                    "prerequisites_met": status.prerequisites_met,
                    "reasons": status.reasons,
                    "critical_blockers": status.critical_blockers
                }
                for b_id, status in results.items()
            }
        }
