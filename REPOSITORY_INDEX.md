# Speed Lock — Complete Repository Index

This master index provides rapid reference across all 39 curated repositories in the archive, categorized by research domain and exploitation methodology.

## Category Quick Links
- [GhostLock / IonStack](#ghostlock--ionstack)
- [DirtyFrag and Related Research](#dirtyfrag-and-related-research)
- [Device-Specific Projects](#device-specific-projects)
- [Root Management Apps](#root-management-apps)
- [Kernel Research & Vulnerability Analysis](#kernel-research--vulnerability-analysis)
- [Reference Catalogues](#reference-catalogues)
- [Device Firmware Forensics & Kernel Extracts](#device-firmware-forensics--kernel-extracts)
- [Speed Lock Unified Android App & Workflows](#speed-lock-unified-android-app--workflows)

---

## GhostLock / IonStack

Core implementations, orchestration frameworks, and foundational exploits leveraging the IonStack / CVE-2026-43499 vulnerability primitives.

| Repository | Canonical Remote | Advertised CVE | Target Platforms | Persistence | Local Directory |
|---|---|---|---|---|---|
| **ghostlock-app** | [YuKongA/ghostlock-app](https://github.com/YuKongA/ghostlock-app) | CVE-2026-43499 | Universal Android GKI (5.10, 5.15, 6.1, 6.6, 6.12) | Per-boot ksud late-load via Shizuku | [`repositories/ghostlock-ionstack/ghostlock-app`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/ghostlock-app) |
| **ghostlock-a17** | [mobilehackinglab/ghostlock-a17](https://github.com/mobilehackinglab/ghostlock-a17) | CVE-2026-43499 | Samsung Galaxy A17 (SM-A175F, GKI 6.12) | Persistent per-boot shell daemon (`g4d`/`g4sh`) | [`repositories/ghostlock-ionstack/ghostlock-a17`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/ghostlock-a17) |
| **GhostLock** | [R0rt1z2/GhostLock](https://github.com/R0rt1z2/GhostLock) | CVE-2026-43499 | Amazon Fire Max 11 & Fire TV Stick 4K Max (Fire OS 8 / Kernel 5.10) | Runtime root | [`repositories/ghostlock-ionstack/GhostLock`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/GhostLock) |
| **IonStackQuest3** | [F-19-F/IonStackQuest3](https://github.com/F-19-F/IonStackQuest3) | CVE-2026-43499 | Meta Quest 3, Quest 3S (Linux 5.10.240) | Per-boot root; contains `gen_ionstack_config.py` vmlinux offset generator | [`repositories/ghostlock-ionstack/IonStackQuest3`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/IonStackQuest3) |

---

## DirtyFrag and Related Research

Implementations, cross-platform ports, Android rooting tools, and detection rules for the deterministic page cache write primitives (CVE-2026-43284 / CVE-2026-43500).

| Repository | Canonical Remote | Research Focus | Target Kernels / Architecture | Key Contribution | Local Directory |
|---|---|---|---|---|---|
| **DFRoot** | [diabl0w/DFRoot](https://github.com/diabl0w/DFRoot) | Android Root Tool | Fire OS 8, Linux 5.10, 5.15, 6.1 | KSU-agnostic module loading without unlocking bootloader; Audited for X6871 ([`DFROOT_X6871_AUDIT.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/device-compatibility/DFROOT_X6871_AUDIT.md)) | [`repositories/dirtyfrag/DFRoot`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DFRoot) |
| **DirtyFrag (mitschud)** | [mitschud/DirtyFrag](https://github.com/mitschud/DirtyFrag) | Exploit Chain | Linux / Android 5.10 - 6.12 | Deterministic xfrm-ESP page cache write implementation | [`repositories/dirtyfrag/DirtyFrag-mitschud`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DirtyFrag-mitschud) |
| **DFReroot** | [polygraphene/DFReroot](https://github.com/polygraphene/DFReroot) | 2nd Stage Root | Galaxy S26 OneUI 8.5 (Kernel 6.6/6.12) | Two-stage persistence using system-UID app | [`repositories/dirtyfrag/DFReroot`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DFReroot) |
| **DirtyInit** | [combeng6th/DirtyInit](https://github.com/combeng6th/DirtyInit) | Init Escalation | Samsung OneUI 3.0 - 8.0 (4.19 - 6.6) | `IpSecManager` + `libbase.so` LogMessage hijack into `u:r:init:s0` | [`repositories/dirtyfrag/DirtyInit`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DirtyInit) |
| **dirtyfrag (V4bel)** | [V4bel/dirtyfrag](https://github.com/V4bel/dirtyfrag) | Original Research | Linux x86_64 / arm64 | Reference PoC for CVE-2026-43284 & CVE-2026-43500 | [`repositories/dirtyfrag/dirtyfrag-V4bel`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/dirtyfrag-V4bel) |
| **dirtyfrag-rs** | [t0asts/dirtyfrag-rs](https://github.com/t0asts/dirtyfrag-rs) | Implementation Port | Linux x86_64 musl | Memory-safe static Rust port | [`repositories/dirtyfrag/dirtyfrag-rs`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/dirtyfrag-rs) |
| **dirtyfrag-arm64** | [linnemanlabs/dirtyfrag-arm64](https://github.com/linnemanlabs/dirtyfrag-arm64) | AArch64 Port & Security Analysis | ARM64 Linux, AWS Graviton | AArch64 validation & Ubuntu AppArmor bypass analysis | [`repositories/dirtyfrag/dirtyfrag-arm64`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/dirtyfrag-arm64) |
| **DirtyFrag-Galaxy** | [coey0814/DirtyFrag-Galaxy](https://github.com/coey0814/DirtyFrag-Galaxy) | Android App | Samsung OneUI 6.x - 8.x (5.15 - 6.6) | One-click KernelSU late-load without PC or ADB | [`repositories/dirtyfrag/DirtyFrag-Galaxy`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DirtyFrag-Galaxy) |
| **Dirty-Frag-hunting** | [0xAllow/Dirty-Frag-hunting](https://github.com/0xAllow/Dirty-Frag-hunting) | Purple Team / SIEM | Multi-platform Linux monitoring | 6 Sigma rules, 5 YARA rules, exposure checking script | [`repositories/dirtyfrag/Dirty-Frag-hunting`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/Dirty-Frag-hunting) |

---

## Device-Specific Projects

Hardware-specific ports, vendor offset definitions, and custom bootloader jailbreaks adapted to OEM firmware distributions.

| Repository | Canonical Remote | OEM Target | Firmware / OS | Kernel Baseline | Local Directory |
|---|---|---|---|---|---|
| **ghostlock-oneplus** | [JoinChang/ghostlock-oneplus](https://github.com/JoinChang/ghostlock-oneplus) | OnePlus 11, 12, Open, Ace 3/Pro | ColorOS / OxygenOS 14 - 16 | GKI 6.1, 6.6 | [`repositories/device-specific-projects/ghostlock-oneplus`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/ghostlock-oneplus) |
| **GhostLock-Galaxy** | [wxxsfxyzm/GhostLock-Galaxy](https://github.com/wxxsfxyzm/GhostLock-Galaxy) | Samsung Galaxy S23, S24, A55 | OneUI 6.1 / OneUI 7 | GKI 6.1, 6.6 | [`repositories/device-specific-projects/GhostLock-Galaxy`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/GhostLock-Galaxy) |
| **GhostSam** | [snothin/GhostSam](https://github.com/snothin/GhostSam) | Samsung Galaxy Exynos & Snapdragon | OneUI 6.0 - 7.0 | 5.15, 6.1, 6.6 | [`repositories/device-specific-projects/GhostSam`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/GhostSam) |
| **ghostlock-emerald** | [datfooldive/ghostlock-emerald](https://github.com/datfooldive/ghostlock-emerald) | POCO M6 Pro (`emerald`) | Xiaomi HyperOS (Android 14) | 5.10, 5.15 | [`repositories/device-specific-projects/ghostlock-emerald`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/ghostlock-emerald) |
| **oppo-ghostlock** | [pubglite55/oppo-ghostlock](https://github.com/pubglite55/oppo-ghostlock) | OPPO Find X7, Reno 11/12 | ColorOS 14 (Android 14) | 5.15, 6.1 | [`repositories/device-specific-projects/oppo-ghostlock`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/oppo-ghostlock) |
| **meizu21-ghostlock-root** | [ymh001/meizu21-ghostlock-root](https://github.com/ymh001/meizu21-ghostlock-root) | Meizu 21, Meizu 21 Pro | Flyme 10.5 (Android 14) | GKI 6.1 | [`repositories/device-specific-projects/meizu21-ghostlock-root`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/meizu21-ghostlock-root) |
| **iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock** | [ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock](https://github.com/ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock) | iQOO Z9 5G, vivo T3 5G | Funtouch OS 14 (Android 14) | 5.15 | [`repositories/device-specific-projects/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock) |
| **Root-My-Galaxy** | [BuSung-dev/Root-My-Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy) | Samsung Galaxy S21 - S25 | Android 12 - 15 | 5.10, 5.15, 6.1 | [`repositories/device-specific-projects/Root-My-Galaxy`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Galaxy) |
| **Root-My-Galaxy-Payloads** | [BuSung-dev/Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads) | Samsung Galaxy Companion | Android 13 - 15 | 5.10, 5.15, 6.1 | [`repositories/device-specific-projects/Root-My-Galaxy-Payloads`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Galaxy-Payloads) |
| **Root-My-Pixel** | [alex193a/Root-My-Pixel](https://github.com/alex193a/Root-My-Pixel) | Google Pixel 6 - 9 Pro | Android 13 - 15 | 5.10, 5.15, 6.1 | [`repositories/device-specific-projects/Root-My-Pixel`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Pixel) |
| **Root-My-Pixel-Payloads** | [alex193a/Root-My-Pixel-Payloads](https://github.com/alex193a/Root-My-Pixel-Payloads) | Google Pixel 6 - 11 Family | Android 14 - 16 | 5.15, 6.1, 6.6 | [`repositories/device-specific-projects/Root-My-Pixel-Payloads`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Pixel-Payloads) |
| **Root-My-Device** | [tqmane/Root-My-Device](https://github.com/tqmane/Root-My-Device) | Nothing, Xiaomi, OnePlus | Android 14 - 17 | 5.10 - 6.6 | [`repositories/device-specific-projects/Root-My-Device`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Device) |
| **root-my-nothing** | [ang3lo-azevedo/root-my-nothing](https://github.com/ang3lo-azevedo/root-my-nothing) | Nothing Phone (1), Phone (2) | Nothing OS 2.5 - 2.6 | 5.4, 5.10, 5.15 | [`repositories/device-specific-projects/root-my-nothing`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/root-my-nothing) |
| **pixel-ksu-root** | [JingMatrix/pixel-ksu-root](https://github.com/JingMatrix/pixel-ksu-root) | Google Pixel 7/8/9, Galaxy A52s | Stock Locked Android 14/15 | 5.10, 5.15, 6.1 | [`repositories/device-specific-projects/pixel-ksu-root`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/pixel-ksu-root) |
| **Root-My-Device-Payloads** | [WitAqua-tools/Root-My-Device-Payloads](https://github.com/WitAqua-tools/Root-My-Device-Payloads) | Multi-device (incl. Quest 3) | Android 12 - 15 | 5.10 - 6.1 | [`repositories/device-specific-projects/Root-My-Device-Payloads`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Device-Payloads) |
| **IonStack-S22U** | [sarabpal-dev/IonStack-S22U](https://github.com/sarabpal-dev/IonStack-S22U) | Samsung Galaxy S22+ (`SM-S906E`) | Android 12 GKI / Android 16 baseband | Kernel 5.10.236 | [`repositories/device-specific-projects/IonStack-S22U`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/IonStack-S22U) |
| **smt878u-ionstack-poc** | [Wtrwx/smt878u-ionstack-poc](https://github.com/Wtrwx/smt878u-ionstack-poc) | Samsung Galaxy Tab S7+ 5G (`SM-T878U`) | One UI (Android 11/12) | Kernel 4.19.113 | [`repositories/device-specific-projects/smt878u-ionstack-poc`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/smt878u-ionstack-poc) |
| **QuestStack** | [starseed12345/QuestStack](https://github.com/starseed12345/QuestStack) | Meta Quest 1 & Quest 2 | Meta Horizon OS | 4.4, 4.19, 5.4 | [`repositories/device-specific-projects/QuestStack`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/QuestStack) |
| **Root-My-Galaxy-SM-S918B** | [soumarcelino/Root-My-Galaxy-SM-S918B](https://github.com/soumarcelino/Root-My-Galaxy-SM-S918B) | Samsung Galaxy S23 Ultra (`SM-S918B`) | One UI 9 Beta 2 (Android 16) | GKI 5.15 | [`repositories/device-specific-projects/Root-My-Galaxy-SM-S918B`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Galaxy-SM-S918B) |
| **Root-My-Device-techtornados** | [techtornados/Root-My-Device](https://github.com/techtornados/Root-My-Device) | Nothing Phone (3a) (`A059` Asteroids) | Nothing OS (Android 16) | GKI 6.1.157 | [`repositories/device-specific-projects/Root-My-Device-techtornados`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Device-techtornados) |

---

## Root Management Apps

Modern userspace-to-root privilege escalation chains integrating userspace logic flaws with kernel late-loading.

| Repository | Canonical Remote | Exploitation Architecture | Platform Target | Persistence Mode | Local Directory |
|---|---|---|---|---|---|
| **speedlock-app** | Local Unified Project | Decoupled Diagnostic Engine + Backend Abstraction (`DFRoot`, `GhostLock`, `DirtyInit`, `UniRoot`) | Infinix GT 20 Pro (`X6871`) / Universal GKI 5.10 | Diagnostic Dashboard & Report Exporter | [`speedlock-app/`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app) |
| **lspromise** | [lsposed/lspromise](https://github.com/lsposed/lspromise) | Android 17 `InCallController` logic 0-day + Kernel 1-day (Zero Memory Corruption) | Google Pixel 10 (Android 17) | Per-boot KernelSU loader | [`repositories/root-management-apps/lspromise`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/root-management-apps/lspromise) |
| **UniRoot** | [kuuky29/UniRoot](https://github.com/kuuky29/UniRoot) | Unprivileged DirtyFrag (CVE-2026-43284) engine + GhostLock profiles | Samsung Galaxy S22 - S26 (5.10 - 6.12) | One-tap root + auto-re-root boot service + on-device ksud patching | [`repositories/root-management-apps/UniRoot`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/root-management-apps/UniRoot) |

---

## Kernel Research & Vulnerability Analysis

Upstream vulnerability research foundations, advisory writeups, and target extraction tools.

| Repository | Canonical Remote | Research Focus | Deliverables | Local Directory |
|---|---|---|---|---|
| **CVE-2026-43499-popsicle** | [x-spy/CVE-2026-43499-popsicle](https://github.com/x-spy/CVE-2026-43499-popsicle) | Xiaomi 17 Pro Max Research | `generate_target.py` extractor for boot.img & xbl_config | [`repositories/kernel-research/CVE-2026-43499-popsicle`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/kernel-research/CVE-2026-43499-popsicle) |
| **CyberMeowfia** | [NebuSec/CyberMeowfia](https://github.com/NebuSec/CyberMeowfia) | Nebula Security Research | Upstream IonStack & V8 vulnerability writeups | [`repositories/kernel-research/CyberMeowfia`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/kernel-research/CyberMeowfia) |

---

## Reference Catalogues

Curated directories and encyclopedic records of Android privilege escalation exploits and tooling.

| Repository | Canonical Remote | Scope | Total Entries | Local Directory |
|---|---|---|---|---|
| **awesome-android-root-exploits** | [DuncanParSky/awesome-android-root-exploits](https://github.com/DuncanParSky/awesome-android-root-exploits) | Historical & modern Android LPE catalog | 20+ documented CVE exploits | [`repositories/reference-catalogues/awesome-android-root-exploits`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/reference-catalogues/awesome-android-root-exploits) |
| **awesome-android-root** | [awesome-android-root/awesome-android-root](https://github.com/awesome-android-root/awesome-android-root) | Ultimate modern Android rooting ecosystem | 650+ root apps, modules, and guides | [`repositories/reference-catalogues/awesome-android-root`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/reference-catalogues/awesome-android-root) |

---

## Device Firmware Forensics & Kernel Extracts

Primary binary evidence and forensic artifacts extracted directly from official stock OEM firmware packages:

| Device Target | Firmware Package | Extracted Kernel / Assets | Reports & Inventories |
|---|---|---|---|
| **Infinix GT 20 Pro (`X6871`)**<br>MediaTek Dimensity 8200 Ultimate (`MT6896`) | Fastboot `X6871-15-31` & Recovery `X6871-15.1.2.180SP05` | Google GKI 2.0 `Image-x6871-5.10.237`, extracted live `.config`, boot/vendor ramdisks, DTBO blobs; Integration: [`integration/x6871/`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/integration/x6871/) | [`X6871_UNIFIED_INTEGRATION.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/device-compatibility/X6871_UNIFIED_INTEGRATION.md)<br>[`X6871_VALIDATION_RESULTS.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/device-compatibility/X6871_VALIDATION_RESULTS.md)<br>[`DFROOT_X6871_AUDIT.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/device-compatibility/DFROOT_X6871_AUDIT.md)<br>[`DFROOT_X6871_COMPATIBILITY_MATRIX.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/device-compatibility/DFROOT_X6871_COMPATIBILITY_MATRIX.md)<br>[`DFROOT_X6871_SUPPORT_PLAN.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/device-compatibility/DFROOT_X6871_SUPPORT_PLAN.md)<br>[`STOCK_FIRMWARE_ANALYSIS.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/firmware/X6871/analysis-reports/STOCK_FIRMWARE_ANALYSIS.md)<br>[`KERNEL_5_10_COMPATIBILITY.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/firmware/X6871/analysis-reports/KERNEL_5_10_COMPATIBILITY.md)<br>[`ROM_INVENTORY.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/firmware/X6871/inventory/ROM_INVENTORY.md) |

---

## Speed Lock Unified Android App & Workflows

| Component | Target Artifacts | Capabilities & Architecture | CI/CD Workflows | Test Status | Local Directory |
|---|---|---|---|---|---|
| **Speed Lock App** (`io.speedlock.app`) | Android Debug & Release APKs | 6 Cohesive Screens (Fluent/Material 3), Decoupled Backend Registry (`DFRoot`, `GhostLock`, `DirtyInit`, `UniRoot`), MT6895 BSP vs MT6896 commercial SoC resolution, 5-tier verification architecture | [`.github/workflows/android-ci.yml`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/.github/workflows/android-ci.yml)<br>[`.github/workflows/android-release.yml`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/.github/workflows/android-release.yml) | 48/48 Passing (30 Python + 18 Java) | [`speedlock-app`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/speedlock-app) |

