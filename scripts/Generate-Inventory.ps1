<#
.SYNOPSIS
    Generate-Inventory.ps1 - Generates and refreshes JSON, CSV, and markdown status tables for Speed Lock.
.DESCRIPTION
    Synthesizes Git metadata, directory layout, upstream history, and technical attributes
    into authoritative inventories:
      - inventory/repositories.json
      - inventory/repositories.csv
      - inventory/clone-status.md
    Logs all operations to logs/inventory_<timestamp>.log.
#>

[CmdletBinding()]
param(
    [string]$WorkspaceRoot = "C:\Users\Admin\Videos\Github\Speed Lock"
)

$ErrorActionPreference = "Continue"

$LogDir = Join-Path $WorkspaceRoot "logs"
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}
$LogFile = Join-Path $LogDir "inventory_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

function Log-Message {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] [$Level] $Message"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log-Message "Starting inventory generation for Speed Lock workspace: $WorkspaceRoot"

# Run discovery
$discoverScript = Join-Path $WorkspaceRoot "scripts\Discover-Repositories.ps1"
$discovered = & $discoverScript -WorkspaceRoot $WorkspaceRoot

Log-Message "Discovered $($discovered.Count) repositories from disk."

# Curated knowledge base of technical attributes
$KnowledgeBase = @{
    "ghostlock-app" = @{
        Classification = "Original Project (Framework/App)";
        CVE = "CVE-2026-43499";
        Targets = "Multi-device GKI kernels (5.10, 5.15, 6.1, 6.6, 6.12)";
        SoCs = "Qualcomm, MediaTek, Tensor, Exynos";
        Android = "Android 14, 15, 16";
        Kernels = "5.10, 5.15, 6.1, 6.6, 6.12";
        Bootloader = "Locked Bootloader (Jailbreak / Runtime root)";
        Persistence = "Per-boot runtime root via KernelSU / ReSukiSU / KowSU / ksud late-load";
        TestingEvidence = "Extensive multi-OEM HOCON kernel profiles; Shizuku integration verified";
        Issues = "Requires Shizuku / adb shell service to execute without app seccomp limits"
    };
    "ghostlock-a17" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "Samsung Galaxy A17 (SM-A175F)";
        SoCs = "MediaTek Helio G99 / Dimensity";
        Android = "Android 16";
        Kernels = "GKI 6.12";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot root shell daemon (g4d / g4sh via Usermode Helper); non-persistent across reboot";
        TestingEvidence = "Verified by MobileHackingLab maintainer; full writeup, dmesg offsets & UMH root log provided";
        Issues = "None; stable per-boot daemon without kernel panic"
    };
    "GhostLock" = @{
        Classification = "Port / Derivative";
        CVE = "CVE-2026-43499";
        Targets = "Amazon Fire Max 11 (sunstone), Fire TV Stick 4K Max 2nd Gen (karat)";
        SoCs = "MediaTek MT8188J / MT7921";
        Android = "Fire OS 8 (Android 11/12 base)";
        Kernels = "Linux 5.10 (8.3.1.9 to 8.3.3.8 builds)";
        Bootloader = "Locked Bootloader";
        Persistence = "Temporary runtime root";
        TestingEvidence = "Maintainer build table covering 11 specific OS build increments & kernel hashes";
        Issues = "None"
    };
    "ghostlock-oneplus" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "OnePlus 12, OnePlus Open, OnePlus Ace 3 / Pro, OnePlus 11";
        SoCs = "Qualcomm Snapdragon 8 Gen 2 / 8 Gen 3";
        Android = "ColorOS / OxygenOS 14, 15, 16";
        Kernels = "6.1, 6.6";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root via UMH / ksud injection";
        TestingEvidence = "Documented device profiles; ColorOS 17 security analysis notes";
        Issues = "Some recent ColorOS builds pending physical device validation"
    };
    "GhostLock-Galaxy" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "Samsung Galaxy S24, S23, A55, Z Fold/Flip series";
        SoCs = "Qualcomm Snapdragon 8 Gen 3, Exynos 2400/1480";
        Android = "OneUI 6.1 / OneUI 7 (Android 14/15)";
        Kernels = "6.1, 6.6";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root";
        TestingEvidence = "Maintainer commit log updates kernel struct offsets for recent OneUI builds";
        Issues = "Offsets require manual recalibration per firmware build"
    };
    "GhostSam" = @{
        Classification = "Device-Specific Port / Multichain";
        CVE = "CVE-2026-43499, CVE-2026-43284";
        Targets = "Samsung Galaxy Exynos & Snapdragon models";
        SoCs = "Exynos 1380, 1480, 2200, 2400; Snapdragon 8 Gen 2/3";
        Android = "OneUI 6.0, 6.1, 7.0";
        Kernels = "5.15, 6.1, 6.6";
        Bootloader = "Locked Bootloader";
        Persistence = "Dual chain: Ghostlock per-boot root or DirtyFrag KMI late-load";
        TestingEvidence = "Release v0.2.3 notes with device compatibility matrix";
        Issues = "None"
    };
    "ghostlock-emerald" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "POCO M6 Pro (emerald)";
        SoCs = "MediaTek Helio G99 Ultra";
        Android = "Xiaomi HyperOS (Android 14)";
        Kernels = "5.10 / 5.15";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root";
        TestingEvidence = "Extracted kernel offsets for POCO M6 Pro HyperOS builds";
        Issues = "Physical validation on latest HyperOS patch level unverified"
    };
    "oppo-ghostlock" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "OPPO Find X7, Reno 11/12 series";
        SoCs = "MediaTek Dimensity 9300, Dimensity 8200";
        Android = "ColorOS 14 (Android 14)";
        Kernels = "5.15, 6.1";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root";
        TestingEvidence = "Community pull requests with confirmed offset tables";
        Issues = "None"
    };
    "meizu21-ghostlock-root" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "Meizu 21, Meizu 21 Pro";
        SoCs = "Snapdragon 8 Gen 3";
        Android = "Flyme 10.5 (Android 14)";
        Kernels = "6.1";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root";
        TestingEvidence = "Flyme 10.5 kernel offsets and bootstrap shell script provided";
        Issues = "None"
    };
    "iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "iQOO Z9 5G, vivo T3 5G";
        SoCs = "MediaTek Dimensity 7200";
        Android = "Funtouch OS 14 (Android 14)";
        Kernels = "5.15";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot root; Shizuku APK launcher";
        TestingEvidence = "Maintainer demo and documentation for Funtouch OS 14";
        Issues = "Disabled default adb root access in latest commit to prevent security lockout"
    };
    "Root-My-Galaxy" = @{
        Classification = "Original Project (Toolkit)";
        CVE = "CVE-2026-43499, CVE-2024-36971, Unspecified";
        Targets = "Samsung Galaxy series (S21 through S25, A series)";
        SoCs = "Exynos & Snapdragon";
        Android = "Android 12 through Android 15";
        Kernels = "5.10, 5.15, 6.1";
        Bootloader = "Locked / Unlocked supported";
        Persistence = "Automated AP/boot patching or runtime payload execution";
        TestingEvidence = "Active maintainer with 560+ merged pull requests across device builds";
        Issues = "None"
    };
    "Root-My-Galaxy-Payloads" = @{
        Classification = "Companion Payload Repository";
        CVE = "CVE-2026-43499, CVE-2024-36971";
        Targets = "Samsung Galaxy devices";
        SoCs = "Exynos & Snapdragon";
        Android = "Android 13, 14, 15";
        Kernels = "5.10, 5.15, 6.1";
        Bootloader = "Locked / Unlocked";
        Persistence = "Precompiled ELF payloads, selinux rules, and kernel modules";
        TestingEvidence = "280+ device-specific payload configs";
        Issues = "Native binary payloads; must not be executed during archival"
    };
    "Root-My-Pixel" = @{
        Classification = "Original Project (Toolkit)";
        CVE = "CVE-2026-43499, Logic Flaws";
        Targets = "Google Pixel 6 through Pixel 9 Pro";
        SoCs = "Google Tensor G1, G2, G3, G4";
        Android = "Android 13, 14, 15";
        Kernels = "5.10, 5.15, 6.1";
        Bootloader = "Locked bootloader supported (temporary root), Unlocked for permanent";
        Persistence = "Temporary root shell / APatch / Magisk boot image integration";
        TestingEvidence = "Maintained by Alessandro Paluzzi; active community issue tracking";
        Issues = "Submodule Root-My-Pixel-Payloads present and initialized"
    };
    "Root-My-Pixel-Payloads" = @{
        Classification = "Companion Payload Repository";
        CVE = "CVE-2026-43499, Kernel primitives";
        Targets = "Google Pixel 6, 7, 8, 9, 10, 11 family";
        SoCs = "Google Tensor family";
        Android = "Android 14, 15, 16";
        Kernels = "5.15, 6.1, 6.6";
        Bootloader = "Locked / Unlocked";
        Persistence = "Native exploit payloads and boot offsets";
        TestingEvidence = "Verified offset tables across monthly Pixel security bulletins";
        Issues = "Native code; archival only"
    };
    "Root-My-Device" = @{
        Classification = "Original Project (Multi-OEM)";
        CVE = "CVE-2026-43499, KernelSU hooks";
        Targets = "Nothing Phone (1, 2, 2a, 3a), Xiaomi, OnePlus, Motorola";
        SoCs = "Qualcomm, MediaTek";
        Android = "Android 14, 15, 17";
        Kernels = "5.10, 5.15, 6.1, 6.6";
        Bootloader = "Locked bootloader jailbreak";
        Persistence = "Integrated KernelSU submodule injection";
        TestingEvidence = "Documented builds for Nothing Phone (3a) Android 17 build";
        Issues = "Submodules KernelSU & Root-My-Device-KSU initialized"
    };
    "root-my-nothing" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "Nothing Phone (1) [spacewar], Nothing Phone (2)";
        SoCs = "Snapdragon 778G+ / 8+ Gen 1";
        Android = "Nothing OS 2.5 / 2.6 (Android 14)";
        Kernels = "5.4 / 5.10 / 5.15";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root";
        TestingEvidence = "Custom Nothing UI integration; verified on Nothing Phone (1) spacewar branch";
        Issues = "None"
    };
    "pixel-ksu-root" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "Google Pixel 7/8/9 series, Samsung Galaxy A52s";
        SoCs = "Google Tensor, Snapdragon 778G";
        Android = "Android 14, 15";
        Kernels = "5.10, 5.15, 6.1";
        Bootloader = "Stock Locked Bootloader";
        Persistence = "Runtime KernelSU memory loader";
        TestingEvidence = "Live device logs and Samsung A52s support verified in commit history";
        Issues = "None"
    };
    "Root-My-Device-Payloads" = @{
        Classification = "Companion Payload Repository (Fork)";
        CVE = "CVE-2026-43499";
        Targets = "Multi-device (Nothing, Xiaomi, Meta Quest 3)";
        SoCs = "Snapdragon XR2 Gen 2, Qualcomm SM series";
        Android = "Android 12 through Android 15";
        Kernels = "5.10, 5.15, 6.1";
        Bootloader = "Locked / Unlocked";
        Persistence = "Precompiled KernelSU payloads";
        TestingEvidence = "Meta Quest 3 KSU payload PR merged";
        Issues = "Submodules KernelSU & Root-My-Device-KSU initialized"
    };
    "DFRoot" = @{
        Classification = "Original Project (Android Tool)";
        CVE = "CVE-2026-43284 (DirtyFrag)";
        Targets = "Fire Max 11 (sunstone), Fire TV Stick 4K Max, Samsung Galaxy, generic Android";
        SoCs = "MediaTek, Qualcomm";
        Android = "Fire OS 8, Android 13, 14";
        Kernels = "5.10, 5.15, 6.1";
        Bootloader = "Locked Bootloader (loads custom kernel module without unlocking)";
        Persistence = "KernelSU-compatible SU manager late-load; soft-reboot persistence";
        TestingEvidence = "Documented Fire OS 8 build targets (RS8336-RS8338) and Galaxy S26 tests";
        Issues = "Vulnerable to manufacturer mitigation or kernel > 6.1 accidental mitigations"
    };
    "DirtyFrag-mitschud" = @{
        Classification = "Original Project (C Implementation)";
        CVE = "CVE-2026-43284 (DirtyFrag)";
        Targets = "Linux Kernel / Android GKI";
        SoCs = "x86_64, arm64";
        Android = "Linux / Android 14+";
        Kernels = "Linux 5.10 - 6.12 (xfrm-ESP vulnerable range)";
        Bootloader = "Not Applicable (Kernel LPE)";
        Persistence = "Page cache overwrite (arbitrary file write as root)";
        TestingEvidence = "Full exploit chain documentation & kernel commit references";
        Issues = "Name disambiguated from V4bel/dirtyfrag on Windows filesystem"
    };
    "DFReroot" = @{
        Classification = "Derivative Tool";
        CVE = "CVE-2026-43284 (DirtyFrag)";
        Targets = "Samsung Galaxy S26 (m1q)";
        SoCs = "Snapdragon 8 Gen 4 / Exynos 2500";
        Android = "OneUI 8.5 (Android 16)";
        Kernels = "6.6 / 6.12";
        Bootloader = "Locked Bootloader";
        Persistence = "Second-stage persistent root: persists system-UID app, then uses DirtyFrag";
        TestingEvidence = "Tested on Galaxy S26 OneUI 8.5 build BP4A.251205.006";
        Issues = "Maintainer recommends diabl0w/DFRoot as primary alternative"
    };
    "DirtyInit" = @{
        Classification = "Original Project (Android Tool)";
        CVE = "CVE-2026-43284 (DirtyFrag)";
        Targets = "Samsung Galaxy series (Note 20 Ultra through S26 Ultra)";
        SoCs = "Snapdragon & Exynos";
        Android = "OneUI 3.0 through OneUI 8.0";
        Kernels = "Linux 4.19 through 6.6";
        Bootloader = "Locked Bootloader";
        Persistence = "Runtime root in u:r:init:s0 namespace via IpSecManager + libbase.so LogMessage hijack; ADB socket relay";
        TestingEvidence = "Detailed writeup explaining init namespace restrictions and OverlayFS limitations";
        Issues = "Init lacks execute_no_trans; cannot execute third-party binaries directly"
    };
    "dirtyfrag-V4bel" = @{
        Classification = "Original Research (Reference PoC)";
        CVE = "CVE-2026-43284, CVE-2026-43500";
        Targets = "Linux Kernel (x86_64 / arm64)";
        SoCs = "x86_64, aarch64";
        Android = "Linux / Android Kernel subsystem";
        Kernels = "Upstream Linux prior to f4c50a4034e6 & aa54b1d27fe0";
        Bootloader = "Not Applicable (Kernel LPE)";
        Persistence = "Page cache overwrite (deterministic, zero race condition)";
        TestingEvidence = "Original vulnerability disclosure by Hyunwoo Kim (@v4bel); writeup & timeline";
        Issues = "Name disambiguated from mitschud/DirtyFrag on Windows filesystem"
    };
    "dirtyfrag-rs" = @{
        Classification = "Port (Rust)";
        CVE = "CVE-2026-43284";
        Targets = "Linux x86_64 (Ubuntu, OpenSUSE, Fedora)";
        SoCs = "x86_64";
        Android = "Linux OS";
        Kernels = "5.x, 6.x";
        Bootloader = "Not Applicable";
        Persistence = "Arbitrary file write";
        TestingEvidence = "GIF demos on Ubuntu, OpenSUSE, Fedora musl build";
        Issues = "None"
    };
    "dirtyfrag-arm64" = @{
        Classification = "Port (AArch64 / Security Analysis)";
        CVE = "CVE-2026-43284, CVE-2026-43500";
        Targets = "AArch64 / ARM64 Linux, AWS Graviton";
        SoCs = "ARM64 / Graviton";
        Android = "Linux ARM64";
        Kernels = "linux-aws 6.17.0-1013-aws, Ubuntu 24.04.4 LTS";
        Bootloader = "Not Applicable";
        Persistence = "Arbitrary file write";
        TestingEvidence = "Tested on AWS Graviton; AppArmor userns bypass writeup included";
        Issues = "None"
    };
    "DirtyFrag-Galaxy" = @{
        Classification = "Device-Specific Port (Android App)";
        CVE = "CVE-2026-43284";
        Targets = "Samsung Galaxy smartphones & tablets";
        SoCs = "Exynos, Snapdragon";
        Android = "OneUI 6.x, OneUI 7.x, OneUI 8.x";
        Kernels = "5.15, 6.1, 6.6";
        Bootloader = "Locked Bootloader";
        Persistence = "One-click temporary KernelSU / KernelSU Next late-load root; no PC/ADB required";
        TestingEvidence = "Release v2.1 with DeX/mirroring soft-restart fixes and KernelSU Next support";
        Issues = "None"
    };
    "Dirty-Frag-hunting" = @{
        Classification = "Defensive / Purple Team Repository";
        CVE = "CVE-2026-43284, CVE-2026-43500";
        Targets = "Linux hosts & monitoring engines (auditd, SIEM, YARA)";
        SoCs = "All";
        Android = "Linux / Android server environments";
        Kernels = "All vulnerable kernel branches";
        Bootloader = "Not Applicable";
        Persistence = "Not Applicable (Defensive)";
        TestingEvidence = "6 Sigma rules, 5 YARA rules, dirtyfrag_hunt.py exposure checker";
        Issues = "None"
    };
    "CVE-2026-43499-popsicle" = @{
        Classification = "Vulnerability Research & PoC";
        CVE = "CVE-2026-43499";
        Targets = "Xiaomi 17 Pro Max (popsicle), Xiaomi 17 Pro, Xiaomi 17 Ultra";
        SoCs = "Snapdragon 8 Gen 4 / 8 Elite";
        Android = "Xiaomi HyperOS 2.0 (Android 16)";
        Kernels = "6.12.23-android16-5-g75e9b1c7ae7c-abogki463945075-4k";
        Bootloader = "Stock Bootloader (Target generation from boot.img / xbl_config.img)";
        Persistence = "Kernel memory exploitation";
        TestingEvidence = "generate_target.py target offset generator and xbl-detect PR merged";
        Issues = "Requires boot.img & xbl_config.img disassembly for target offsets"
    };
    "CyberMeowfia" = @{
        Classification = "Upstream Vulnerability Research";
        CVE = "CVE-2026-43499, CVE-2026-7899, IonStack";
        Targets = "V8, Android Linux Kernel, Ion subsystem";
        SoCs = "Qualcomm, MediaTek, ARM";
        Android = "Android 14, 15, 16";
        Kernels = "5.10, 5.15, 6.1, 6.6, 6.12";
        Bootloader = "Upstream Research Foundation";
        Persistence = "Vulnerability writeups & bug lists (1000+ entries)";
        TestingEvidence = "Nebula Security research publications and proof-of-concepts";
        Issues = "None"
    };
    "lspromise" = @{
        Classification = "Original Exploit Chain (Zero Memory Corruption)";
        CVE = "Android 17 Telecom 0-day + Kernel 1-day";
        Targets = "Google Pixel 10 (official Android 17 release); incompatible with Pixel 6a (6.1.xxx-android14)";
        SoCs = "Google Tensor G5";
        Android = "Android 17 (Initial official release)";
        Kernels = "6.6, 6.12";
        Bootloader = "Stock Locked Bootloader";
        Persistence = "Per-boot KernelSU loader with 100% deterministic success (no race condition / heap spray)";
        TestingEvidence = "Video demo (VID_20260804_231937_915.mp4) and technical writeup for InCallController.java logic flaw";
        Issues = "Requires reboot if re-running after previous kernel execution"
    };
    "awesome-android-root-exploits" = @{
        Classification = "Reference Catalogue";
        CVE = "Historical (CVE-2014-3153, CVE-2015-3636, CVE-2016-5195, CVE-2019-2215, etc.)";
        Targets = "Comprehensive historical Android devices (Android 2.2 to 14+)";
        SoCs = "All historical architectures (arm, arm64, x86)";
        Android = "Android 2.2 through Android 14";
        Kernels = "2.6 through 6.x";
        Bootloader = "Locked and unlocked bypass catalog";
        Persistence = "Catalogue documentation";
        TestingEvidence = "Extensive reference links to XDA, GitHub, and academic whitepapers";
        Issues = "None"
    };
    "awesome-android-root" = @{
        Classification = "Reference Catalogue & Directory";
        CVE = "Not Applicable (Tooling catalogue)";
        Targets = "Universal Android ecosystem";
        SoCs = "All";
        Android = "Android 5.0 through Android 16";
        Kernels = "All";
        Bootloader = "Guide references for locked and unlocked devices";
        Persistence = "650+ root tools (Magisk, KernelSU, APatch, LSPosed)";
        TestingEvidence = "Live web portal and verified community module links";
        Issues = "None"
    };
    "IonStack-S22U" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499";
        Targets = "Samsung Galaxy S22+ (SM-S906E, g0q)";
        SoCs = "Snapdragon 8 Gen 1 (taro / SM8450)";
        Android = "Android 12 GKI / Android 16 baseband (S906EXXSEGZE3)";
        Kernels = "5.10.236-android12-9-31998796-abS906EXXSEGZE3";
        Bootloader = "Locked Bootloader (Runtime root)";
        Persistence = "Per-boot runtime root via in-process pselect stamp & forged work_struct UMH";
        TestingEvidence = "Verified on physical SM-S906E hardware (1/16 attempts); target_generator included";
        Issues = "Requires custom target.h generated from kernel Image"
    };
    "IonStackQuest3" = @{
        Classification = "Device-Specific Port / Framework";
        CVE = "CVE-2026-43499";
        Targets = "Meta Quest 3, Meta Quest 3S";
        SoCs = "Snapdragon XR2 Gen 2 (SM8550-VR)";
        Android = "Meta Horizon OS (Android 12/14 base)";
        Kernels = "Linux 5.10.240-g69827d40d782";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root; patched in incremental 52345320040100520";
        TestingEvidence = "Device verified on Meta Quest 3; includes gen_ionstack_config.py for vmlinux symbol extraction";
        Issues = "Meta patch deployed in firmware build 52345320040100520"
    };
    "smt878u-ionstack-poc" = @{
        Classification = "Research PoC (Unfinished)";
        CVE = "CVE-2026-43499";
        Targets = "Samsung Galaxy Tab S7+ 5G (SM-T878U / gts7l)";
        SoCs = "Snapdragon 865+ (kona)";
        Android = "OneUI (Android 11/12)";
        Kernels = "4.19.113-27114284 (T878USQS8DXE1)";
        Bootloader = "Locked Bootloader";
        Persistence = "WIP (Failed rtmutex.c:585 lock acquisition; no root landed)";
        TestingEvidence = "Detailed negative results and RTMUTEX_WEAPONIZATION.md analysis across 470 test runs";
        Issues = "Incomplete; stack-reclaim payload fails lock acquisition at rtmutex.c:585"
    };
    "QuestStack" = @{
        Classification = "Toolkit / Application";
        CVE = "CVE-2026-43499, CVE-2021-1931";
        Targets = "Meta Quest 1, Meta Quest 2";
        SoCs = "Snapdragon 835, Snapdragon XR2 Gen 1";
        Android = "Meta Horizon OS";
        Kernels = "4.4, 4.19, 5.4";
        Bootloader = "Locked to Unlocked (exploits fastboot CVE-2021-1931 from temporary root)";
        Persistence = "Permanent bootloader unlock + persistent root";
        TestingEvidence = "Public desktop GUI application released for Win/Mac/Linux; verified on firmware 49845030443200410";
        Issues = "Quest Pro unsupported"
    };
    "Root-My-Galaxy-SM-S918B" = @{
        Classification = "Device-Specific Port";
        CVE = "CVE-2026-43499, Kernel primitives";
        Targets = "Samsung Galaxy S23 Ultra (SM-S918B, SM-S918N, dm3q)";
        SoCs = "Snapdragon 8 Gen 2 for Galaxy (SM8550-AC)";
        Android = "One UI 9 Beta 2 (S918BXXUAZZI8) / Android 16";
        Kernels = "5.15 GKI";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root via BOPE engine (2-second execution) + KernelSU Next v3.4.0";
        TestingEvidence = "Verified on SM-S918B running S918BXXUAZZI8 One UI 9 Beta 2";
        Issues = "Tied strictly to exact firmware builds"
    };
    "Root-My-Device-techtornados" = @{
        Classification = "Fork / Derivative Port";
        CVE = "CVE-2026-43499";
        Targets = "Nothing Phone (3a) [Asteroids / A059]";
        SoCs = "MediaTek Dimensity 7200 Pro";
        Android = "Nothing OS (Android 16 / SDK 36)";
        Kernels = "6.1.157-android14-11-g82d681c9b06b-ab14634535";
        Bootloader = "Locked Bootloader";
        Persistence = "Per-boot runtime root + KernelSU 32525 late-load";
        TestingEvidence = "Maintainer device-verified on build B4.1-260618-1048";
        Issues = "Fork isolates Nothing Phone (3a) from multi-device root"
    };
    "UniRoot" = @{
        Classification = "Original Project (Android App)";
        CVE = "CVE-2026-43284, CVE-2026-43499";
        Targets = "Samsung Galaxy series (S22 through S26, tablets)";
        SoCs = "Snapdragon & Exynos";
        Android = "OneUI 4.x through OneUI 8.x";
        Kernels = "5.10, 5.15, 6.1, 6.6, 6.12";
        Bootloader = "Locked Bootloader (zero bootloader unlock required)";
        Persistence = "One-tap runtime root with boot service auto-re-root; patches ksud on-device with KDP+DEFEX bypass LKMs";
        TestingEvidence = "Release V5 with IpSecManager SA allocator, progress UI, and live su probe";
        Issues = "None"
    };
}

$InventoryList = @()

foreach ($repo in $discovered) {
    $kb = $KnowledgeBase[$repo.Name]
    if (-not $kb) {
        $kb = @{
            Classification = "Research Repository";
            CVE = "Unverified";
            Targets = "Unverified";
            SoCs = "Unknown";
            Android = "Unknown";
            Kernels = "Unknown";
            Bootloader = "Unverified";
            Persistence = "Unverified";
            TestingEvidence = "None provided";
            Issues = "None documented"
        }
    }

    $entry = [ordered]@{
        Name                   = $repo.Name
        Category               = $repo.Category
        CanonicalUrl           = $repo.RemoteUrl
        LocalClonePath         = $repo.LocalPath
        RelativePath           = $repo.RelativePath
        UpstreamBranch         = $repo.Branch
        CommitSha              = $repo.CommitSha
        ShortSha               = $repo.ShortSha
        LastCommitDate         = $repo.CommitDate
        LastCommitAuthor       = $repo.Author
        LastCommitSubject      = $repo.Subject
        Classification         = $kb.Classification
        AdvertisedCVE          = $kb.CVE
        TargetDevices          = $kb.Targets
        TargetSoCs             = $kb.SoCs
        TargetAndroidVersions  = $kb.Android
        TargetKernelVersions   = $kb.Kernels
        BootloaderRequirement  = $kb.Bootloader
        PersistenceMechanism   = $kb.Persistence
        TestingEvidence        = $kb.TestingEvidence
        CloneStatus            = "Cloned (Verified)"
        WorkingTreeDirty       = $repo.IsDirty
        HasSubmodules          = $repo.HasSubmodules
        SubmoduleCount         = $repo.SubmoduleCount
        HasLFS                 = $repo.HasLFS
        IdentifiedIssues       = $kb.Issues
    }

    $InventoryList += [PSCustomObject]$entry
}

# 1. Output inventory/repositories.json
$jsonPath = Join-Path $WorkspaceRoot "inventory\repositories.json"
$InventoryList | ConvertTo-Json -Depth 6 | Set-Content -Path $jsonPath -Encoding UTF8
Log-Message "Generated: $jsonPath"

# 2. Output inventory/repositories.csv
$csvPath = Join-Path $WorkspaceRoot "inventory\repositories.csv"
$InventoryList | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
Log-Message "Generated: $csvPath"

Log-Message "Inventory refresh completed successfully!"
return $InventoryList
