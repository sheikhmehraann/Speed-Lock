"""
backend_registry.py

Registry for discovering, managing, and querying exploit research backends.
"""

from typing import Dict, List, Optional
from ..core.backend_interface import IExploitBackend
from .dfroot_backend import DFRootBackend
from .ghostlock_backend import GhostLockBackend

class BackendRegistry:
    def __init__(self):
        self._backends: Dict[str, IExploitBackend] = {}
        self._register_default_backends()

    def _register_default_backends(self) -> None:
        self.register(DFRootBackend())
        self.register(GhostLockBackend())

    def register(self, backend: IExploitBackend) -> None:
        self._backends[backend.backend_id] = backend

    def get(self, backend_id: str) -> Optional[IExploitBackend]:
        return self._backends.get(backend_id)

    def list_backends(self) -> List[IExploitBackend]:
        return list(self._backends.values())

    def get_by_cve(self, cve_id: str) -> List[IExploitBackend]:
        return [b for b in self._backends.values() if b.vulnerability_cve.lower() == cve_id.lower()]
