# Speed Lock — Infinix GT 20 Pro (X6871) Deep Kernel 5.10 Security Research

**Target Device:** Infinix GT 20 Pro  
**Model Number:** `X6871`  
**Manufacturer:** Infinix Mobile / Transsion Holdings  
**SoC:** MediaTek Dimensity 8200 Ultimate (`MT6896`)  
**Document Path:** `research/device-compatibility/X6871_KERNEL_RESEARCH.md`  
**Status:** In-Depth Architecture & Feasibility Report  

---

## 1. Hardware & Platform Specifications

| Component | Technical Detail | Relevance to Kernel Research |
|---|---|---|
| **SoC** | MediaTek Dimensity 8200 Ultimate (`MT6896`) | TSMC 4nm process; MediaTek architecture utilizing out-of-tree hardware drivers |
| **CPU Cluster** | 1x Arm Cortex-A78 @ 3.1 GHz<br>3x Arm Cortex-A78 @ 3.0 GHz<br>4x Arm Cortex-A55 @ 2.0 GHz | ARMv8.2-A 64-bit architecture; hardware enforcement of PXN (Privileged Execute Never) and PAN (Privileged Access Never) |
| **GPU** | Arm Mali-G610 MC6 | Bifrost/Valhall GPU driver interface via `/dev/mali0` |
| **Co-processor** | Pixelworks X5 Turbo Gaming Display Processor | Proprietary display management hardware |
| **Page Size** | **4096 bytes (4KB)** | Standard page size for MT6896 5.10 kernels; directly compatible with 4KB exploit memory layouts |
| **Storage / RAM** | 256GB UFS 3.1 / 8GB or 12GB LPDDR5X | High-throughput storage; standard Linux block layer |

---

## 2. Firmware Lineage & Kernel Environment

### Observed Firmware Builds & Kernel Lineage
- **Shipped Operating System:** Android 14 with Transsion XOS 14 (e.g. `X6871-GL`, `X6871-IN`, `X6871-RU` regional variants).
- **Reported Kernel Version:** Linux Kernel **`5.10.x`** (observed builds: `5.10.198` and `5.10.226`).
- **Kernel Lineage:**
  - Unlike devices launching on pure Android 14 with Generic Kernel Image (GKI) 6.1 or 6.6, the Infinix GT 20 Pro carries forward a **MediaTek vendor BSP kernel** branched from the `android12-5.10` / `android13-5.10` common tree.
  - While base GKI symbols exist, MediaTek integrates proprietary sub-modules: Command Queue (`CMDQ`), Camera Control Unit (`CCU`), Multi-Media Memory Management Unit (`M4U`), and custom PowerVR/Mali schedulers.

### GPLv2 Compliance Status
- **Source Availability:** As reported across the Android security and developer community (e.g. XDA discussions), Transsion has not published a self-contained, easily accessible public kernel source repository specifically for the `X6871`.
- **Consequence for Research:** Compiling custom out-of-tree kernel modules (such as an exact-matching KernelSU LKM) cannot rely on a pre-existing vendor source tree. Instead, symbol CRCs and structural offsets must be derived **directly from the stock kernel binary (`Image`)**.

---

## 3. Platform Mitigations & Constraints

| Security Boundary | Mechanism | Impact on Privilege Escalation |
|---|---|---|
| **Bootloader & AVB 2.0** | Android Verified Boot with hardware-backed public key | Unlocking requires `fastboot flashing unlock`, triggering a complete userdata factory reset. Furthermore, Infinix does not provide an official unlock portal. **Locked-bootloader root requires an in-memory LPE**. |
| **dm-verity** | SHA-256 Merkle tree verification on `/system`, `/vendor`, `/product` | **Zero-modification rule:** No partition may be mounted read-write or altered on disk. Doing so trips AVB dm-verity and results in a permanent device brick/bootloop. |
| **SELinux** | Enforcing domain separation | Restricts untrusted apps from direct access to sensitive device nodes; untrusted apps cannot open raw network sockets. |
| **Clang CFI** | Control Flow Integrity (`CONFIG_CFI_CLANG=y`) | Indirect function calls are validated against compile-time jump tables (`.cfi_jt`). Exploits cannot redirect function pointers to arbitrary kernel code without utilizing valid CFI stubs or data-only attacks. |
| **KASLR** | Kernel Address Space Layout Randomization | Randomizes `_text` base address on each boot. Exploit requires an address leak primitive to calculate the ASLR slide. |
| **KDP / RKP / PAN** | Hardware page table protections & Read-Only Creds | `struct cred` structures are frequently mapped in read-only memory protected by ARM EL2 hypervisor or EL3 secure monitor. **Overwriting `cred->uid` directly causes an instant kernel crash**. |

---

## 4. Exploit Chain Feasibility Assessment for X6871

### Chain 1: GhostLock / IonStack (CVE-2026-43499) — **High Plausibility Lead**

1. **Vulnerability Mechanics:**
   - A race condition during PI futex unblocking (`futex_requeue_pi` / `rt_mutex`) causes a use-after-free on the kernel stack.
   - The race is converted into an 8-byte arbitrary kernel memory write.
2. **MediaTek Precedent:**
   - The `GhostLock` project (`repositories/ghostlock-ionstack/GhostLock`) maintains a dedicated `5.10` branch that successfully exploits MediaTek MT8188J and MT7921 vendor kernels on Fire OS 8.
   - Sibling Transsion models in the Infinix GT family (Infinix GT 30 `X6876` and GT 30 Pro `X6873`) are explicitly listed in `ghostlock-app`'s profile database.
3. **Bypassing KDP / Read-Only Creds:**
   - IonStack does not modify `struct cred` directly.
   - Instead, it constructs a forged `work_struct` and queues it onto the global kernel workqueue (`system_unbound_wq`), setting its entry point to `call_usermodehelper_exec_work`.
   - The kernel spawns a userspace helper executable (`/data/local/tmp/su_daemon`) directly as `uid=0`, `gid=0` in the `init` SELinux domain.
4. **Prerequisites for X6871 Porting:**
   - Requires extracting the kernel `Image` from the stock `boot.img`.
   - Running `gen_ionstack_config.py` (from `IonStackQuest3`) or `target_generator` (from `IonStack-S22U`) to resolve:
     - `kimage_text_base` (`_text`)
     - `random_misc_fops` (or `ashmem_misc_fops`)
     - `call_usermodehelper_exec_work`
     - `system_unbound_wq`
     - `cfi_jt` entries for indirect calls.

### Chain 2: DirtyFrag (CVE-2026-43284) — **Moderate Plausibility Lead**

1. **Vulnerability Mechanics:**
   - Exploits out-of-bounds page-cache corruption in the IPsec ESP packet fragmentation handler (`xfrm-ESP`).
   - Unprivileged Android applications trigger this by using the public Android SDK API `android.net.IpSecManager` to allocate IPsec Security Associations (as demonstrated in `UniRoot` and `DirtyInit`).
2. **LKM Late-Loading Challenge:**
   - DirtyFrag on Samsung Galaxy devices uses page-cache corruption to load a precompiled KernelSU kernel module (`.ko`).
   - For the Infinix GT 20 Pro, loading a `.ko` requires that the module's `vermagic` and symbol CRC table match the MT6896 kernel exactly.
   - Because GKI KMI on MediaTek vendor trees can deviate from standard Google GKI kernels, generic GKI 5.10 modules (`dfroot-android12-5.10.ko` / `dirtyfrag-android12-5.10.ko`) will be rejected by `init_module` with `-EPERM` or `-ENOEXEC` unless custom-compiled for the specific MT6896 CRC table.

---

## 5. Technical Comparison: Transsion Sibling Profiles

The `ghostlock-app` repository documents the following sibling Transsion devices:

| Device Model | Model Code | SoC | Kernel Release | Page Size | Profile Status in Archive |
|---|---|---|---|---|---|
| **Infinix Note 50s 5G** | Transsion Note | MediaTek Dimensity | `6.1.145-android14-11` | 4096 | Supported (`ghostlock-app`) |
| **Infinix GT 30** | `X6876` | MediaTek Dimensity | `6.1.145-android14-11` | 4096 | Supported (`ghostlock-app`) |
| **Infinix GT 30 Pro** | `X6873` | MediaTek Dimensity 8300 | `6.1.157-android14-11` | 4096 | Supported (`ghostlock-app`) |
| **Infinix GT 20 Pro** | **`X6871`** | **MediaTek Dimensity 8200 Ultimate** | **`5.10.198` / `5.10.226`** | **4096** | **Target Profile Missing (Requires Extraction)** |

---

## 6. Missing Evidence & Research Checklist

To progress from theoretical feasibility to an actionable target profile for the `X6871`, the following static evidence must be gathered:

1. [ ] **Firmware Extraction:**
   - Obtain an official stock firmware package (`X6871_..._V...zip`).
   - Extract `boot.img` and decompress the kernel `Image`.
2. [ ] **Kernel Header & Symbol Extraction:**
   - Execute `kallsyms` symbol extraction on the uncompressed kernel `Image`.
   - Validate whether `/proc/config.gz` has `CONFIG_KALLSYMS_ALL=y` and `CONFIG_CFI_CLANG=y`.
3. [ ] **Target Offset Generation:**
   - Run `IonStackQuest3/gen_ionstack_config.py` on the extracted `Image` to generate `ionstack.conf`.
   - Verify that all 14 mandatory symbol offsets resolve cleanly.
4. [ ] **Security Patch Level (SPL) Check:**
   - Inspect `ro.build.version.security_patch` in `/system/build.prop`.
   - If the patch date is prior to August 2026, CVE-2026-43499 is unpatched.
5. [ ] **SELinux Context Verification:**
   - Confirm domain permissions for untrusted app domains or `shell` domain (`uid=2000`) via `sepolicy-analyze`.

---

## 7. Conclusions & Strategic Recommendation

1. **No Hardware Barrier:** The MediaTek Dimensity 8200 Ultimate (`MT6896`) architecture and standard 4KB page size present no fundamental barrier to in-memory kernel privilege escalation.
2. **Best Candidate Chain:** **CVE-2026-43499 (IonStack / GhostLock)** via Usermode Helper (`call_usermodehelper_exec_work`) is the most viable path because:
   - It has been proven on MediaTek 5.10 vendor kernels (`GhostLock` 5.10 branch).
   - It bypasses EL2/KDP read-only credential protections completely.
   - It avoids the strict LKM symbol CRC requirements of KernelSU module loading by providing a native socket root daemon (`su_daemon`).
3. **Execution Safety:** All testing must remain purely static until exact target offsets are extracted and verified against the specific running build.
