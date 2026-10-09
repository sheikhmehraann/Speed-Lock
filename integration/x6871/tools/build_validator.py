"""
build_validator.py

Static and environment validator for native build chains, kernel module Kbuild
prerequisites, and cross-project repository dependencies.
Detects missing toolchains and verifies structural readiness for target X6871 builds.
"""

import os
import shutil
from dataclasses import dataclass, field
from typing import Dict, List, Any, Optional

@dataclass
class ToolchainStatus:
    tool: str
    available: bool
    path: Optional[str] = None
    version_output: Optional[str] = None

@dataclass
class BuildValidationReport:
    workspace_root: str
    host_toolchain: Dict[str, ToolchainStatus] = field(default_factory=dict)
    repository_status: Dict[str, Dict[str, Any]] = field(default_factory=dict)
    kbuild_readiness: Dict[str, Any] = field(default_factory=dict)
    can_build_apk: bool = False
    can_build_native_jni: bool = False
    can_build_lkm: bool = False
    summary_verdict: str = "BLOCKED"
    blockers: List[str] = field(default_factory=list)

class BuildValidator:
    def __init__(self, workspace_root: str):
        self.workspace_root = workspace_root

    def check_toolchains(self) -> Dict[str, ToolchainStatus]:
        tools_to_check = [
            "clang",
            "make",
            "gcc",
            "javac",
            "java",
            "rustc",
            "aarch64-linux-gnu-gcc",
            "ndk-build"
        ]
        results = {}
        for tool in tools_to_check:
            loc = shutil.which(tool)
            results[tool] = ToolchainStatus(
                tool=tool,
                available=loc is not None,
                path=loc
            )
        return results

    def audit_repositories(self) -> Dict[str, Dict[str, Any]]:
        repos = {
            "DFRoot": {
                "path": os.path.join(self.workspace_root, "repositories", "dirtyfrag", "DFRoot"),
                "required_files": [
                    os.path.join("app", "src", "main", "jni", "exp.c"),
                    os.path.join("app", "src", "main", "jni", "splicehelper.c"),
                    os.path.join("app", "src", "main", "jni", "libcxx.S"),
                    os.path.join("lkm", "dfroot.c"),
                    os.path.join("lkm", "Makefile")
                ]
            },
            "DirtyInit": {
                "path": os.path.join(self.workspace_root, "repositories", "dirtyfrag", "DirtyInit"),
                "required_files": [
                    os.path.join("native", "dirtyinit.c"),
                    os.path.join("native", "dfi_exploit.c")
                ]
            },
            "GhostLock": {
                "path": os.path.join(self.workspace_root, "repositories", "ghostlock-ionstack", "GhostLock"),
                "required_files": [
                    os.path.join("src", "rootchain.c")
                ]
            },
            "ghostlock-app": {
                "path": os.path.join(self.workspace_root, "repositories", "ghostlock-ionstack", "ghostlock-app"),
                "required_files": [
                    os.path.join("src", "CMakeLists.txt"),
                    os.path.join("src", "core", "main.cpp"),
                    os.path.join("tools", "mtk-phys", "mtk-phys.sh")
                ]
            },
            "UniRoot": {
                "path": os.path.join(self.workspace_root, "repositories", "root-management-apps", "UniRoot"),
                "required_files": [
                    os.path.join("app", "build.gradle.kts"),
                    os.path.join("build.gradle.kts")
                ]
            }
        }

        audits = {}
        for name, spec in repos.items():
            base = spec["path"]
            exists = os.path.isdir(base)
            missing_files = []
            if exists:
                for rel in spec["required_files"]:
                    if not os.path.exists(os.path.join(base, rel)):
                        missing_files.append(rel)
            audits[name] = {
                "directory_exists": exists,
                "all_files_present": exists and len(missing_files) == 0,
                "missing_files": missing_files
            }
        return audits

    def check_kbuild_readiness(self) -> Dict[str, Any]:
        kdir = os.environ.get("KDIR")
        ndk_home = os.environ.get("ANDROID_NDK_HOME") or os.environ.get("ANDROID_NDK_ROOT")
        return {
            "KDIR_set": bool(kdir),
            "KDIR_path": kdir,
            "ANDROID_NDK_set": bool(ndk_home),
            "ANDROID_NDK_path": ndk_home,
            "has_ack_kernel_tree": False
        }

    def validate_all(self) -> BuildValidationReport:
        toolchains = self.check_toolchains()
        repo_audits = self.audit_repositories()
        kbuild = self.check_kbuild_readiness()

        report = BuildValidationReport(
            workspace_root=self.workspace_root,
            host_toolchain=toolchains,
            repository_status=repo_audits,
            kbuild_readiness=kbuild
        )

        # Evaluate capabilities
        has_java = toolchains.get("javac", ToolchainStatus("javac", False)).available
        has_clang = toolchains.get("clang", ToolchainStatus("clang", False)).available
        has_make = toolchains.get("make", ToolchainStatus("make", False)).available
        has_ndk = bool(kbuild.get("ANDROID_NDK_set"))

        report.can_build_apk = has_java and has_ndk
        report.can_build_native_jni = has_ndk and (has_clang or has_make)
        report.can_build_lkm = has_make and bool(kbuild.get("KDIR_set"))

        if not has_java:
            report.blockers.append("JDK/javac missing: cannot compile Android APK packaging")
        if not has_ndk:
            report.blockers.append("Android NDK missing: cannot compile JNI native binaries (exp.c, splicehelper.c)")
        if not kbuild.get("KDIR_set"):
            report.blockers.append("Kernel tree KDIR missing: cannot compile loadable kernel module (dfroot.ko)")
        if not has_clang:
            report.blockers.append("Host Clang compiler missing: required for LLVM=1 kernel module compilation")

        all_repos_ok = all(r["all_files_present"] for r in repo_audits.values())
        if not all_repos_ok:
            report.blockers.append("One or more repository source dependencies are incomplete")

        if not report.blockers:
            report.summary_verdict = "READY"
        elif all_repos_ok and (not report.can_build_lkm or not report.can_build_native_jni):
            report.summary_verdict = "SOURCE_AUDITED_TOOLCHAIN_MISSING"
        else:
            report.summary_verdict = "BLOCKED"

        return report
