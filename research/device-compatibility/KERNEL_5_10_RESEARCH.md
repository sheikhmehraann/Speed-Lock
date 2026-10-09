# Speed Lock — Linux Kernel 5.10 & Android 5.10 Root Compatibility Matrix

**Archive Location:** `research/device-compatibility/KERNEL_5_10_RESEARCH.md`  
**Classification:** Defensive Static Kernel Security Research & Compatibility Review  
**Target Scope:** Linux Kernel 5.10 upstream, Android Common Kernel (ACK) 5.10 GKI, and OEM Vendor Kernels  
**Primary Research Device:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896`)

---

## 1. Executive Summary & Kernel 5.10 Architectural Context

The Linux 5.10 LTS cycle represents a foundational turning point in Android platform security:
1. **Introduction of GKI 2.0 (Generic Kernel Image):** Android 12 marked Google's mandate for GKI 2.0. On compliant devices launching with Android 12, the core kernel resides in the boot partition as a unified generic image (`boot.img`), enforcing a stable Kernel Module Interface (KMI).
2. **The Vendor Kernel Exception:** Many MediaTek devices (such as Dimensity 8100, 8200, 9000 series) and specialized platforms (Amazon Fire OS, Meta Horizon OS) utilize OEM vendor-forked BSP kernels derived from early `android12-5.10` or `android13-5.10` trees. These vendor kernels frequently break strict GKI KMI compliance by embedding custom proprietary drivers directly or altering symbol CRC tables.
3. **Primary Exploitation Vectors on 5.10:**
   - **CVE-2026-43499 (IonStack / GhostLock):** Futex Priority Inheritance (`futex_requeue_pi` / `rt_mutex`) race condition causing a use-after-free (UAF) on the kernel stack. It yields an 8-byte arbitrary kernel write, converted to arbitrary kernel R/W via `miscdevice`/`file_operations` hijacking, followed by executing a forged `work_struct` via `call_usermodehelper_exec_work`.
   - **CVE-2026-43284 (DirtyFrag):** Kernel page-cache corruption via IPsec ESP packet fragmentation (`xfrm-ESP`). Unprivileged Android applications invoke `IpSecManager` to allocate security associations, enabling deterministic corruption of cached pages (e.g. injecting kernel modules or overwriting read-only userspace binaries).

---

## 2. Kernel 5.10 Comprehensive Compatibility Matrix

The table below compiles every project in the Speed Lock archive containing source files, precompiled modules, target profiles, or documented research addressing Linux kernel 5.10.

| Project & Upstream URL | Vulnerability | Kernel Release | Android Branch | Architecture | SoC / Chipset | Tested Devices & Firmware | Root Method | Bootloader Constraint | Persistence | Status | X6871 Relevance | Primary Evidence |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **GhostLock (5.10 Branch)**<br>`https://github.com/R0rt1z2/GhostLock` | CVE-2026-43499 | Linux 5.10.x (`-ge7a6aa0ea53f` to `-gabbd3a8e8dee`) | Fire OS 8 Vendor Kernel | ARM64 (`aarch64`) | MediaTek MT8188J, MediaTek MT7921 / MT8696 | Amazon Fire Max 11 (`sunstone`), Fire TV Stick 4K Max 2nd Gen (`karat`) (Fire OS 8.1.4.5 - 8.3.3.8) | Race condition -> 8-byte store -> `miscdevice->fops` hijack -> forged `work_struct` -> `call_usermodehelper` root shell | **Locked Bootloader** | Temporary (per-boot; daemon on port 9999/9060) | **Source-Verified & Device-Tested** | **Plausible Research Lead** (Proves CVE-2026-43499 succeeds on MediaTek 5.10 vendor kernels) | [`GhostLock/src/rootchain.c`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/GhostLock/src/rootchain.c#L1-L60), [`GhostLock/README.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/GhostLock/README.md) |
| **IonStack-S22U**<br>`https://github.com/sarabpal-dev/IonStack-S22U` | CVE-2026-43499 | `5.10.236-android12-9-31998796-abS906EXXSEGZE3` | Android 12 GKI (`android12-5.10`) | ARM64 (`aarch64`) | Snapdragon 8 Gen 1 (`taro` / SM8450) | Samsung Galaxy S22+ (`SM-S906E` / `g0q`), build `BP2A.250605.031.A3.S906EXXSEGZE3` | In-process `pselect` stamp route -> CFI verification -> KASLR slide -> pipe physrw -> UMH root helper | **Locked Bootloader** | Per-boot temporary root (`uid=0`, permissive SELinux) | **Device-Tested** (Verified on physical hardware 1/16 attempts) | **Plausible Research Lead** (Provides ARM64 5.10 in-process `pselect` route and `target_generator` tool) | [`IonStack-S22U/PORT-S906EXXSEGZE3.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/IonStack-S22U/PORT-S906EXXSEGZE3.md#L1-L45), [`target_generator`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/IonStack-S22U/target_generator) |
| **IonStackQuest3**<br>`https://github.com/F-19-F/IonStackQuest3` | CVE-2026-43499 | `5.10.240-g69827d40d782` | Meta Horizon OS (Android 12/14 base) | ARM64 (`aarch64`) | Snapdragon XR2 Gen 2 (`SM8550-VR`) | Meta Quest 3, firmware incremental `52168470043600520` | Futex PI UAF -> mm_struct order-2 reclaim -> `kimage_text_base` calculation -> UMH root | **Locked Bootloader** | Per-boot temporary root | **Device-Tested** (Patched in incremental `52345320040100520`) | **Plausible Research Lead** (Contains `gen_ionstack_config.py` for automated ARM64 vmlinux symbol resolution) | [`IonStackQuest3/README.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/IonStackQuest3/README.md#L1-L40), [`gen_ionstack_config.py`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/IonStackQuest3/gen_ionstack_config.py#L1-L60) |
| **DirtyFrag-mitschud**<br>`https://github.com/mitschud/DirtyFrag` | CVE-2026-43284 | Linux 5.10 - 6.12 | Android Common Kernel GKI | ARM64 (`aarch64`), x86_64 | Multi-platform | Generic Android 12 / Android 13 GKI 5.10 | Page cache overwrite via xfrm-ESP fragment handling -> LKM injection | **Locked Bootloader** | Per-boot KernelSU LKM late-load | **Binary-Verified** (Ships prebuilt `dfroot-android12-5.10.ko` and `dfroot-android13-5.10.ko`) | **Plausible Research Lead** (Demonstrates LKM late-loading feasibility on GKI 5.10; vendor KMI mismatch must be checked) | [`DirtyFrag-mitschud/app/src/main/jni/ko/`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DirtyFrag-mitschud/app/src/main/jni/ko/) |
| **DirtyFrag-Galaxy**<br>`https://github.com/coey0814/DirtyFrag-Galaxy` | CVE-2026-43284 | Linux 5.10 - 6.18 | One UI 4.x / 5.x / 6.x / 7.x | ARM64 (`aarch64`) | Exynos & Snapdragon | Samsung Galaxy S22 series, Galaxy A series | IpSecManager -> ESP fragmentation -> page cache corruption -> live-loads KernelSU LKM | **Locked Bootloader** | One-click per-boot temporary root | **Binary-Verified** (Ships `dirtyfrag-android12-5.10.ko`, `dirtyfrag-android13-5.10.ko`) | **Plausible Research Lead** (Precompiled 5.10 LKMs available) | [`DirtyFrag-Galaxy/app/src/main/jni/ko/`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DirtyFrag-Galaxy/app/src/main/jni/ko/) |
| **UniRoot**<br>`https://github.com/kuuky29/UniRoot` | CVE-2026-43284, CVE-2026-43499 | Linux 5.10 - 6.12 | One UI 4.x - 8.x | ARM64 (`aarch64`) | Exynos, Snapdragon | Samsung Galaxy S22, S23, S24, S25, S26 | Unprivileged DirtyFrag engine + IpSec SA allocation + on-device ksud patching with KDP+DEFEX LKMs | **Locked Bootloader** | Automated per-boot re-root service | **Binary-Verified** (Ships 8 precompiled 5.10 LKMs including `dfr_lkm-android12-5.10.ko` and `android12-5.10_kernelsu.ko`) | **Plausible Research Lead** (Complete unprivileged Android app implementation) | [`UniRoot/README.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/root-management-apps/UniRoot/README.md#L1-L45) |
| **GhostSam**<br>`https://github.com/snothin/GhostSam` | CVE-2026-43499, CVE-2026-43284 | Linux 5.10 - 6.18 | One UI 4.x - 7.0 | ARM64 (`aarch64`) | Snapdragon 8 Gen 1, Exynos 2200 | Samsung Galaxy S22 (`s22`), S26, Z Fold8 | Dual-chain: DirtyFrag KMI module load or Ghostlock futex PI UAF -> forged `work_struct` -> `su_daemon` socket | **Locked Bootloader** | Per-boot temporary root; ksud bind-mount over dormant system binary | **Source-Verified** | **Plausible Research Lead** (Documents DEFEX and KDP bypass methods on 5.10) | [`GhostSam/README.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/GhostSam/README.md#L1-L35) |
| **Root-My-Galaxy-Payloads**<br>`https://github.com/BuSung-dev/Root-My-Galaxy-Payloads` | CVE-2026-43499 | Linux 5.10 | One UI 4.x / 5.x | ARM64 (`aarch64`) | Exynos & Snapdragon | Samsung Galaxy A53 5G (`SM-A536E`), Galaxy Z Fold4 (`SM-F9360`) | KernelSU late-load module injection | **Locked Bootloader** | Temporary runtime root | **Binary-Verified** (Ships `android12-5.10_kernelsu-A536EXXSNGZG3-kdp.ko`, `android12-5.10_kernelsu-samsung-kdp.ko`) | **Plausible Research Lead** (Provides exact KMI module build configurations for 5.10) | [`Root-My-Galaxy-Payloads`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/Root-My-Galaxy-Payloads) |
| **ghostlock-app**<br>`https://github.com/YuKongA/ghostlock-app` | CVE-2026-43499 | 5.15.41 through 6.12.38 (Transsion models: 6.1.145, 6.1.157) | Android 13, 14, 15, 16 | ARM64 (`aarch64`) | MediaTek Dimensity 8300 / 9300, Qualcomm, Tensor | Infinix Note 50s 5G, Infinix GT 30 (`X6876`), Infinix GT 30 Pro (`X6873`) | Shizuku-driven or local APK IonStack exploitation -> KernelSU late-load | **Locked Bootloader** | Per-boot temporary root | **Target-Verified** (Transsion GT 30 series supported, but X6871 profile omitted) | **Direct Architectural Lead** (Contains Transsion/Infinix profile format and vendor boot parsing) | [`ghostlock-app/docs/kernel_profiles/SUPPORTED_DEVICES.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/ghostlock-ionstack/ghostlock-app/docs/kernel_profiles/SUPPORTED_DEVICES.md#L1-L60) |
| **DFReroot**<br>`https://github.com/polygraphene/DFReroot` | CVE-2026-43284 | Linux 5.10 - 6.12 | One UI 5.x - 8.5 | ARM64 (`aarch64`) | Snapdragon & Exynos | Galaxy S22, S26 | DirtyFrag page cache corruption -> KernelSU deployment | **Locked Bootloader** | Temporary root | **Binary-Verified** (Contains `dirtyfrag-android12-5.10.ko`) | **Plausible Research Lead** (Precompiled module reference) | [`DFReroot/app/src/main/jni/ko/`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/DFReroot/app/src/main/jni/ko/) |
| **pixel-ksu-root**<br>`https://github.com/JingMatrix/pixel-ksu-root` | CVE-2026-43499 | Linux 5.10 - 6.1 | Android 13 / 14 | ARM64 (`aarch64`) | Tensor G1 / G2, Snapdragon 778G | Pixel 6/7, Galaxy A52s | Runtime KernelSU memory loader | **Locked Bootloader** | Temporary runtime root | **Source-Verified** | **Plausible Research Lead** (Reference memory loader for KernelSU on 5.10) | [`pixel-ksu-root/README.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/pixel-ksu-root/README.md) |
| **smt878u-ionstack-poc**<br>`https://github.com/Wtrwx/smt878u-ionstack-poc` | CVE-2026-43499 | Linux 4.19.113 | One UI 3.x / 4.x | ARM64 (`aarch64`) | Snapdragon 865+ (`kona`) | Samsung Galaxy Tab S7+ 5G (`SM-T878U`) | Pure-C Host-assisted IonStack PoC | **Locked Bootloader** | None achieved | **Experimental / Unfinished** (Fails lock acquisition at `rtmutex.c:585`) | **Research Negative Reference** (Highlights pitfalls in weaponizing `rt_mutex` on non-GKI vendor kernels) | [`smt878u-ionstack-poc/docs/RTMUTEX_WEAPONIZATION.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/repositories/device-specific-projects/smt878u-ionstack-poc/docs/RTMUTEX_WEAPONIZATION.md) |

---

## 3. Upstream Patch Analysis & Vulnerability Timeline

### CVE-2026-43499 (IonStack / GhostLock)
- **Vulnerable Subsystem:** `kernel/futex.c` and `kernel/locking/rtmutex.c` (specifically `futex_requeue_pi`, `rt_mutex_adjust_prio_chain`, and unblock refcount handling).
- **Vulnerability Lifetime:** Introduced in legacy Linux PI futex implementations; affected all versions up to 6.12/6.14 before CVE disclosure.
- **Backport History:** Upstream kernel stable trees backported the fix in late Q2 / Q3 2026. OEM firmware updates with Android Security Patch Level **prior to August 2026** generally remain completely unpatched and vulnerable.
- **Weaponization across Vendors:**
  - **Qualcomm (Snapdragon):** Standard `mm_struct` order-2 reclaim or `pselect` stamp route.
  - **MediaTek (MT8188J, MT7921):** Confirmed weaponized in `GhostLock` 5.10 branch using `q3slide.c` and forged `work_struct` queued on `system_unbound_wq`.

### CVE-2026-43284 (DirtyFrag)
- **Vulnerable Subsystem:** `net/xfrm/xfrm_input.c` and ESP packet fragmentation handler.
- **Vulnerability Lifetime:** Affects Linux 5.10 through 6.18 upstream prior to commit `f4c50a4034e6` and `aa54b1d27fe0`.
- **Android Context:** Unprivileged apps can trigger ESP socket processing via the public Android Java API `android.net.IpSecManager`. This circumvents Android's seccomp and SELinux filters restricting untrusted applications from opening raw sockets.

---

## 4. Key Takeaways for Research

1. **Kernel 5.10 is Extensively Exploitable Without Unlocking Bootloaders:**
   Multiple independent projects (`GhostLock` 5.10 branch, `IonStack-S22U`, `DirtyFrag-mitschud`, `DirtyFrag-Galaxy`, `UniRoot`) prove that locked-bootloader devices running Linux kernel 5.10 can achieve temporary root access using either CVE-2026-43499 or CVE-2026-43284.
2. **MediaTek 5.10 Exploitation is Precedented:**
   The `GhostLock` repository contains verified working exploit targets for MediaTek MT8188J and MT7921 running 5.10 vendor kernels.
3. **Authoritative Ground-Truth for Infinix GT 20 Pro (`X6871`):**
   Stock ROM forensics of the official firmware (`Infinix-GT-20-Pro-X6871-15-31`) directly confirm:
   - **Kernel Version:** Standard Google GKI 2.0 `5.10.237-android12-9-00014-gf82f7360927e-ab14119954`, compiled September 17, 2025.
   - **Patch Level:** AVB 2.0 descriptor registers `security_patch: 2026-07-01`, placing it solidly prior to the July/August 2026 upstream fixes for CVE-2026-43499 and CVE-2026-43284 (**Definitively Vulnerable**).
   - **Module Signing:** `# CONFIG_MODULE_SIG is not set` — kernel module cryptographic signatures are **not enforced**, enabling runtime KernelSU LKM late-load.
   - **Architecture:** 4KB pages, 39-bit VA space (`CONFIG_ARM64_VA_BITS=39`), mirroring the Galaxy S22+ (`IonStack-S22U`).
   - Detailed forensic reports:
     - [`research/firmware/X6871/analysis-reports/STOCK_FIRMWARE_ANALYSIS.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/firmware/X6871/analysis-reports/STOCK_FIRMWARE_ANALYSIS.md)
     - [`research/firmware/X6871/analysis-reports/KERNEL_5_10_COMPATIBILITY.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/firmware/X6871/analysis-reports/KERNEL_5_10_COMPATIBILITY.md)
     - [`research/firmware/X6871/inventory/ROM_INVENTORY.md`](file:///c:/Users/Admin/Videos/Github/Speed%20Lock/research/firmware/X6871/inventory/ROM_INVENTORY.md)

