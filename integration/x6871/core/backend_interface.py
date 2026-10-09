"""
backend_interface.py

Defines the isolated plugin/backend contract for exploit and rooting implementations
integrated into the Speed Lock framework.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from enum import Enum
from typing import List, Dict, Any, Optional

class BackendState(str, Enum):
    """
    Explicit lifecycle states for exploit/rooting backends.
    Strictly prevents marking a backend supported merely because it compiles.
    """
    UNSUPPORTED = "UNSUPPORTED"                 # Hardware/SoC/patched kernel fundamentally incompatible
    UNTESTED = "UNTESTED"                       # Firmware or environment metadata missing/unconfirmed
    METADATA_COMPATIBLE = "METADATA_COMPATIBLE" # High-level config/subsystem matches, but binary ABI/payload unverified
    ABI_COMPATIBLE = "ABI_COMPATIBLE"           # Binaries/LKMs match target vermagic, symbols, and relocations
    VERIFIED_CONFIRMED = "VERIFIED_CONFIRMED"   # Verified via authorized live physical device execution

@dataclass
class BackendStatus:
    backend_id: str
    display_name: str
    state: BackendState
    prerequisites_met: bool
    reasons: List[str] = field(default_factory=list)
    critical_blockers: List[str] = field(default_factory=list)
    diagnostics: Dict[str, Any] = field(default_factory=dict)

    def is_actionable(self) -> bool:
        """Only ABI_COMPATIBLE or VERIFIED_CONFIRMED may be staged or deployed."""
        return self.state in (BackendState.ABI_COMPATIBLE, BackendState.VERIFIED_CONFIRMED)

class IExploitBackend(ABC):
    """
    Abstract interface for independent exploit research backends.
    Keeps each upstream implementation completely isolated.
    """

    @property
    @abstractmethod
    def backend_id(self) -> str:
        """Unique slug identifier for the backend (e.g. 'dfroot', 'ghostlock')."""
        pass

    @property
    @abstractmethod
    def display_name(self) -> str:
        """Human-readable display name."""
        pass

    @property
    @abstractmethod
    def vulnerability_cve(self) -> str:
        """CVE identifier addressed by the backend."""
        pass

    @property
    @abstractmethod
    def supported_architectures(self) -> List[str]:
        """List of supported CPU architectures (e.g. ['arm64'])."""
        pass

    @abstractmethod
    def evaluate_compatibility(self, profile: Any) -> BackendStatus:
        """
        Evaluate compatibility against a DeviceProfile.
        Must return a structured BackendStatus with clear reasons and blockers.
        """
        pass

    @abstractmethod
    def verify_artifacts(self, workspace_root: str) -> Dict[str, bool]:
        """
        Verify that required prebuilt or compiled artifacts exist and pass basic integrity checks.
        """
        pass
