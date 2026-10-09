"""
firmware_inspector.py

High-level static inspector for extracted stock firmware partitions and metadata.
"""

import os
from typing import Dict, Any, List
from ..core.device_identifier import DeviceIdentifier, DeviceProfile

class FirmwareInspector:
    def __init__(self, firmware_dir: str):
        self.firmware_dir = firmware_dir
        self.profile: DeviceProfile = DeviceProfile()

    def inspect(self) -> DeviceProfile:
        self.profile = DeviceIdentifier.from_firmware_directory(self.firmware_dir)
        return self.profile

    def get_summary(self) -> Dict[str, Any]:
        if not self.profile.kernel_release:
            self.inspect()

        return {
            "firmware_dir": self.firmware_dir,
            "device": f"{self.profile.brand} {self.profile.model}",
            "soc": self.profile.soc,
            "kernel_release": self.profile.kernel_release,
            "vermagic": self.profile.vermagic,
            "architecture": self.profile.architecture,
            "page_size": self.profile.page_size_bytes,
            "va_bits": self.profile.va_bits,
            "candidate_libraries": self.profile.candidate_libraries,
            "has_insmod_symlink": self.profile.has_insmod_symlink,
            "has_super_partition": self.profile.has_super_partition,
            "total_config_flags_parsed": len(self.profile.config_flags)
        }
