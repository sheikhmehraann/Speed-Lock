# Speed Lock — DFRoot vs. Archive Projects: Multi-Project Compatibility Matrix

**Report Location:** `research/device-compatibility/DFROOT_X6871_COMPATIBILITY_MATRIX.md`  
**Classification:** Comparative Architecture Analysis & Compatibility Cross-Reference  
**Target Hardware:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896`)  
**Target Kernel:** Linux 5.10.237 GKI 2.0 (`5.10.237-android12-9-00014-gf82f7360927e-ab14119954`)  
**Scope:** DFRoot compared against all 9 major root/LPE research implementations in Speed Lock  

---

## 1. Executive Summary & Epistemological Taxonomy

To ensure rigorous adherence to empirical evidence, every assertion in this matrix is classified into one of four epistemological categories:
- **[Source-Verified Fact]:** Directly proven by static analysis of checked-in source code, build scripts, or extracted firmware binaries in the archive.
- **[Maintainer Claim]:** Stated in repository documentation, commit messages, or release notes, but unverified on independent hardware.
- **[Inference]:** Deductions based on proven Linux kernel mechanics, AOSP specifications, or compiler behavior.
- **[Unknown]:** Empirical questions that require dynamic physical device telemetry to verify.

---

## 2. Multi-Project Technical Comparison Matrix

| Project | Vulnerability Class | Kernel & Android Range | GKI vs Vendor Kernel | Architecture & Page Size | Module ABI / KMI Stance | SELinux & OEM Defenses Addressed | Bootloader Requirement | Reusable Components for X6871 | Compatibility Verdict for X6871 |
|---|---|---|---|---|---|---|---|---|---|
| **DFRoot**<br>`diabl0w/DFRoot` | CVE-2026-43284 (DirtyFrag xfrm-ESP) | Linux 5.10 - 6.18<br>Android 12 - 17 | GKI 2.0 focused | ARM64 (`aarch64`)<br>4KB pages | Stripped LKM; requires matching `vermagic`; dynamic kallsyms via kprobe | Samsung DEFEX hooks, Oppo/OnePlus module unload, init hook via `libc++.so` | **Locked Bootloader** | `splicehelper`, `crash_dump64` bridge, `elf_parser.c`, `bootstrap.c` RO protection | **High Architectural Potential**; blocked only by LKM vermagic mismatch. |
| **DirtyFrag-mitschud**<br>`mitschud/DirtyFrag` | CVE-2026-43284 (DirtyFrag xfrm-ESP) | Linux 5.10 - 6.12<br>Android 12 - 14 | Generic GKI | ARM64 & x86_64<br>4KB pages | Ships prebuilt `dfroot-android12-5.10.ko` (`vermagic=5.10.252-dirty`) | Targets generic `crash_dump64` and `libc++.so` | **Locked Bootloader** | Reference C implementation of DirtyFrag CBC encryption engine | **Identical Engine to DFRoot**; exhibits same vermagic blocker. |
| **DirtyFrag-Galaxy**<br>`coey0814/DirtyFrag-Galaxy` | CVE-2026-43284 (DirtyFrag xfrm-ESP) | Linux 5.10 - 6.18<br>OneUI 6 - 8 | Samsung OEM GKI | ARM64 (`aarch64`)<br>4KB pages | Prebuilt Samsung-targeted LKMs (`dirtyfrag-android12-5.10.ko`) | Samsung RKP, Knox DEFEX, KDP memory protection | **Locked Bootloader** | One-click Java UI orchestration; automated retry loops | **Samsung-Specific**; prebuilt modules contain Samsung RKP bypasses unneeded on MTK. |
| **DirtyInit**<br>`combeng6th/DirtyInit` | CVE-2026-43284 (DirtyFrag xfrm-ESP) | Linux 4.19 - 6.6<br>Android 11 - 15 | Generic GKI & Vendor | ARM64 (`aarch64`)<br>4KB pages | No LKM; pure userspace socket relay | Bypasses SELinux `execute_no_trans` in `init` using Unix stream sockets | **Locked Bootloader** | Non-LKM init escalation route via `libbase.so` (`LogMessage`) | **Alternative Vector Lead**; eliminates LKM module vermagic issue entirely. |
| **DFReroot**<br>`polygraphene/DFReroot` | CVE-2026-43284 (DirtyFrag xfrm-ESP) | Linux 5.10 - 6.12<br>OneUI 5 - 8.5 | Samsung OEM GKI | ARM64 (`aarch64`)<br>4KB pages | Drops LKM to disk or caches system-priv app | Samsung Knox, soft-reboot persistence | **Locked Bootloader** | System-priv app privilege staging; soft-reboot daemon | **Samsung-Specific**; requires soft-reboot flow. |
| **UniRoot**<br>`kuuky29/UniRoot` | CVE-2026-43284 & CVE-2026-43499 | Linux 5.10 - 6.12<br>OneUI 4 - 8 | Samsung OEM GKI | ARM64 (`aarch64`)<br>4KB pages | Bundles 8 prebuilt LKMs with dynamic modversions patcher | Comprehensive Samsung Knox DEFEX/KDP | **Locked Bootloader** | Automated on-device APK packaging and payload installer | **Valuable Reference** for multi-kernel module management. |
| **GhostLock**<br>`R0rt1z2/GhostLock` | CVE-2026-43499 (IonStack / Futex PI) | Linux 5.10 vendor<br>Fire OS 8 | MediaTek MT8188J, MT8696 vendor | ARM64 (`aarch64`)<br>4KB pages | No LKM; memory corruption -> UMH `system_unbound_wq` | Bypasses SELinux via `call_usermodehelper` spawning root daemon | **Locked Bootloader** | MediaTek-specific futex timing parameters (`q3slide.c`) | **Strong MediaTek Precedent**; proves 5.10 MTK futex exploitation. |
| **ghostlock-app**<br>`YuKongA/ghostlock-app` | CVE-2026-43499 (IonStack / Futex PI) | Linux 5.15 - 6.12<br>Android 13 - 16 | Transsion Dimensity (6.1), Qualcomm, Tensor | ARM64 (`aarch64`)<br>4KB pages | Loads KernelSU via Shizuku/local memory write | Transsion security daemons; Samsung Knox; SELinux | **Locked Bootloader** | Official Transsion device profile schema (`X6876`, `X6873`) | **Ecosystem Kinship**; Infinix GT sister device profile structure directly portable. |
| **IonStack-S22U**<br>`sarabpal-dev/IonStack-S22U` | CVE-2026-43499 (IonStack / Futex PI) | `5.10.236` GKI 2.0<br>OneUI 7 (Android 14) | Google GKI 2.0 | ARM64 (`aarch64`)<br>4KB pages, 39-bit VA | No LKM; `pselect` stamp -> UMH root script | Clang CFI + Shadow Call Stack (SCS) bypassed via data-only UMH worker | **Locked Bootloader** | ARM64 39-bit VA memory reclaim and CFI-safe UMH worker route | **Direct Kernel Twin**; `5.10.236` is within 1 patch level of X6871's `5.10.237`. |
| **IonStackQuest3**<br>`F-19-F/IonStackQuest3` | CVE-2026-43499 (IonStack / Futex PI) | `5.10.240` GKI 2.0<br>Horizon OS (A12/14) | Google GKI 2.0 | ARM64 (`aarch64`)<br>4KB pages | No LKM; `mm_struct` order-2 reclaim | SELinux permissive via memory write | **Locked Bootloader** | `gen_ionstack_config.py` automated symbol offset extractor | **Essential Tooling**; automated symbol extraction from `Image-x6871-5.10.237`. |

---

## 3. Detailed Component Reusability & Architecture Alignment

### A. DirtyFrag vs. IonStack Primitives for X6871
- **DirtyFrag (DFRoot, DirtyFrag-mitschud, UniRoot):**
  - **Pros:** 100% deterministic write primitive. Zero race conditions. Bypasses KASLR, Clang CFI, Shadow Call Stack, and SMEP/SMAP without memory leaks or sprays.
  - **Cons:** Requires a valid LKM or executable target that can be executed from a privileged SELinux domain. Under locked bootloaders, it depends on `check_modinfo` vermagic compliance.
- **IonStack (GhostLock, IonStack-S22U, ghostlock-app):**
  - **Pros:** Does not require loadable kernel modules. Writes directly to kernel heap/stack to achieve arbitrary code execution via CFI-compliant usermode helper tasks (`call_usermodehelper_exec_work`).
  - **Cons:** Highly sensitive to struct member offsets and race window timing. Failure causes immediate kernel panic and device reboot.

### B. Module ABI & KMI Reusability Assessment
- **Fact [Source-Verified]:** The X6871 kernel has `# CONFIG_MODULE_SIG is not set`. Cryptographic signature verification is inactive.
- **Fact [Source-Verified]:** The X6871 kernel has `CONFIG_MODVERSIONS=y`.
- **Fact [Source-Verified]:** `dfroot.c` imports only 6 fundamental kernel symbols:
  `register_kprobe`, `unregister_kprobe`, `printk`, `__stack_chk_fail`, `__stack_chk_guard`, `memset`.
- **Inference:** Because the X6871 kernel is an authentic Google ACK GKI 2.0 release (`ab14119954`), these 6 symbols have standard GKI CRCs. If the vermagic string is aligned to `5.10.237-android12-9-00014-gf82f7360927e-ab14119954`, `dfroot.ko` will successfully load into the live kernel without compilation against proprietary MediaTek BSP headers!

---

## 4. Synthesis of Known Mitigations & Incompatibilities

| Potential Mitigation | Presence on X6871 | Impact on DFRoot | Mitigation Workaround |
|---|---|---|---|
| **Kernel Module Signatures** | **Disabled** (`# CONFIG_MODULE_SIG is not set`) | None | None needed. |
| **Clang CFI** | **Enabled** (`CONFIG_CFI_CLANG=y`) | None on DirtyFrag page cache write; LKM code is CFI-compliant | Standard kernel execution. |
| **Shadow Call Stack** | **Enabled** (`CONFIG_SHADOW_CALL_STACK=y`) | None | Page cache write does not corrupt call frames. |
| **Static UMH Path** | **Enabled** (`CONFIG_STATIC_USERMODEHELPER_PATH=""`) | Overridden | `dfroot.c:124` explicitly overrides `((struct subprocess_info *)info)->path = sh`. |
| **Samsung DEFEX / RKP** | **Absent** (MediaTek hardware) | None | DFRoot skips Samsung DEFEX hooks gracefully. |
| **Oppo Hardening LKMs** | **Absent** (Transsion hardware) | None | DFRoot `rmmod` commands fail silently (`2>/dev/null`). |
| **Vermagic Enforcement** | **Strict** (`# CONFIG_MODULE_FORCE_LOAD is not set`) | **FATAL BLOCKER** for prebuilt `.ko` | Must rebuild or binary-patch `.modinfo` vermagic. |
| **AVB 2.0 dm-verity** | **Enforcing** (`flags: 0x0`, rollback 2) | Forces temporary/in-memory root | Handled: DFRoot uses `ksud late-load` in RAM. |
