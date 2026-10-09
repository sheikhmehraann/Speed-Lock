# Speed Lock — Infinix GT 20 Pro (`X6871`) Kernel 5.10 Compatibility & Exploit Feasibility Report

**Report Location:** `research/firmware/X6871/analysis-reports/KERNEL_5_10_COMPATIBILITY.md`  
**Classification:** Defensive Static Kernel Vulnerability Assessment & Compatibility Analysis  
**Target Hardware:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896` / `MT6895` series)  
**Analyzed Binary:** Extracted Stock Kernel Image `Image-x6871-5.10.237` (SHA-256: `613741DE...`)  
**Firmware Baseline:** Fastboot `X6871-15-31` / Recovery `X6871-15.1.2.180SP05-OP001PF001AZ` (AVB Patch: `2026-07-01`)  
**Reference Research:** `GhostLock`, `IonStack-S22U`, `IonStackQuest3`, `DirtyFrag`, `UniRoot`, `ghostlock-app`

---

## 1. Executive Summary & Ground-Truth Correlation

Previous assessments assumed the Infinix GT 20 Pro (`X6871`) ran an out-of-tree MediaTek Board Support Package (BSP) vendor kernel (such as `5.10.198` or `5.10.226`) with proprietary driver modifications and strict module signing. 

Direct forensic extraction of the stock boot and vendor images refutes those assumptions and establishes critical ground-truth findings:

1. **Standard Google GKI 2.0 Core:**
   - The kernel binary is an authentic **Google Android Common Kernel (ACK) GKI 2.0 release**:  
     `Linux version 5.10.237-android12-9-00014-gf82f7360927e-ab14119954`
   - Compiled by Google CI (`ab14119954`) with Clang 12.0.5 on **September 17, 2025**.
   - MediaTek hardware drivers are completely externalized into `vendor_boot` and `vendor_dlkm` as Loadable Kernel Modules (`.ko`).
2. **Definitive Pre-Patch Vulnerability Window:**
   - The Android Verified Boot (AVB 2.0) descriptor registers `security_patch: 2026-07-01`.
   - The upstream security fixes for **CVE-2026-43499 (IonStack / GhostLock)** were backported in late July and August 2026.
   - The kernel was compiled in September 2025. **The stock X6871 kernel is definitively unpatched and vulnerable to CVE-2026-43499**.
3. **Kernel Module Signing is Disabled (`# CONFIG_MODULE_SIG is not set`):**
   - The extracted kernel `.config` proves that cryptographic module signature verification is **disabled**.
   - The kernel will load any ARM64 ELF kernel module provided its symbol version CRCs (`modversions`) match the GKI 2.0 KMI export table.
4. **Locked-Bootloader Compatibility:**
   - Because AVB 2.0 is strictly enforcing (`flags: 0x0`, rollback index 2), static on-disk partition modification will trigger a red-screen dm-verity boot halt.
   - Locked-bootloader privilege escalation must rely strictly on **in-memory runtime exploitation** (GhostLock UMH execution or DirtyFrag page-cache LKM injection).

---

## 2. Kernel Security Architecture vs. Exploit Primitives

The table below contrasts the extracted X6871 kernel configuration against the hard requirements of modern Android kernel exploit chains:

| Kernel Configuration Parameter | X6871 Ground-Truth Value | CVE-2026-43499 (GhostLock / IonStack) Feasibility | CVE-2026-43284 (DirtyFrag) Feasibility | Runtime KernelSU / LKM Late-Load Feasibility |
|---|---|---|---|---|
| **Kernel Version** | `5.10.237` | **Fully Vulnerable:** Upstream fix not merged until late July/August 2026. | **Fully Vulnerable:** Upstream fix not merged until August 2026. | Standard Android 12 GKI 5.10 KMI baseline. |
| **Page Size** | **4096 bytes (4KB)** (`CONFIG_ARM64_4K_PAGES=y`) | **Compatible:** Standard 4KB memory allocation; aligns with all ARM64 IonStack PoCs. | **Compatible:** Page cache size is 4KB; matches DirtyFrag chunking. | Matches standard 4KB precompiled `.ko` modules. |
| **Virtual Address (VA) Bits** | **39 bits** (`CONFIG_ARM64_VA_BITS=39`) | **Identical to S22+:** Exact same memory layout as Samsung Galaxy S22+ (`IonStack-S22U`). | No impact on page cache overwrite. | No impact on standard LKM loading. |
| **Clang CFI** | **`CONFIG_CFI_CLANG=y`** | **Mitigated but Bypassed:** Indirect function calls are validated. Requires data-only attack or targeting valid indirect targets like `call_usermodehelper_exec_work`. | **Not Affected:** DirtyFrag modifies cached disk pages; no indirect calls involved during corruption. | **Not Affected:** Standard module loading via `init_module()` goes through valid kernel code paths. |
| **Shadow Call Stack (SCS)** | **`CONFIG_SHADOW_CALL_STACK=y`** | **Mitigated:** Stack return addresses protected. Precludes traditional ROP chains; exploit must rely on `work_struct` queuing. | **Not Affected:** Overwrites file contents in page cache, not call stack frames. | **Not Affected:** LKM execution does not corrupt call frames. |
| **KASLR** | **`CONFIG_RANDOMIZE_BASE=y`** | **Requires Leak:** Exploit must resolve KASLR slide via pointer leak (e.g., pipe buffer or `q3slide`). | **Bypassed:** Overwriting filesystem page cache does not depend on kernel virtual address randomization. | Resolved dynamically once root / LKM loader is triggered. |
| **Module Signature Enforcement** | **`# CONFIG_MODULE_SIG is not set`** | N/A | **Critical Enabler:** Allows injected LKM to load without OEM private RSA signature! | **100% Permissive:** Only `modversions` symbol CRC validation required. |
| **FUTEX Subsystem** | **`CONFIG_FUTEX=y`** | **Target Present:** PI futex race condition primitive is fully compiled into the kernel. | N/A | N/A |
| **IPsec XFRM / ESP** | **`CONFIG_XFRM=y`, `CONFIG_INET_ESP=y`** | N/A | **Target Present:** ESP fragmentation handling is active in network stack. | N/A |
| **SELinux Enforcement** | **`CONFIG_SECURITY_SELINUX=y`** | Exploit must zero out `selinux_state` or spawn usermode helper in `u:r:kernel:s0`. | Exploit must target init-accessible binaries or inject SELinux bypass into LKM. | KernelSU hooks `security_file_permission` to grant root domain transition. |

---

## 3. Exploit Vector Feasibility Deep-Dive

### Vector A: CVE-2026-43499 (GhostLock / IonStack)

#### 1. Vulnerability Mechanics on X6871
- **Vulnerability Class:** Priority Inheritance Futex (`rt_mutex`) race condition leading to a use-after-free (UAF) on the kernel stack.
- **Trigger Path:** User-space threads race `futex(FUTEX_LOCK_PI)` and `futex(FUTEX_UNLOCK_PI)` / `futex_requeue_pi` across CPU cores.
- **Primitive Acquired:** 8-byte arbitrary kernel write or stale stack reference.

#### 2. CFI & SCS Bypass Strategy (Data-Only Attack)
Because `CONFIG_CFI_CLANG=y` and `CONFIG_SHADOW_CALL_STACK=y` are both active in the X6871 kernel:
- **What Fails:** Traditional ROP chains or arbitrary function pointer hijacking will trigger a CFI trap (`BRK 0x8000`) or panic on stack return.
- **What Succeeds:** The approach proven in `IonStack-S22U` (which also has CFI + SCS enabled on 5.10):
  1. Overwrite a `work_struct` or `miscdevice` structure in kernel heap memory.
  2. Direct the work function pointer to `call_usermodehelper_exec_work`.
  3. Because `call_usermodehelper_exec_work` is an authorized, CFI-valid work function in the kernel image, CFI validation passes without violation.
  4. The worker executes an arbitrary userspace script (e.g. `/data/local/tmp/root.sh`) under the root kernel context (`u:r:kernel:s0`).

#### 3. Memory Layout & Reclaim
- With `CONFIG_ARM64_VA_BITS=39` and `CONFIG_ARM64_4K_PAGES=y`, the X6871 memory architecture is identical to the Samsung Galaxy S22+ (`IonStack-S22U`).
- The order-2 `mm_struct` spray or in-process `pselect` stamp route documented in `IonStack-S22U` can be directly mapped to the X6871 kernel.

---

### Vector B: CVE-2026-43284 (DirtyFrag)

#### 1. Vulnerability Mechanics on X6871
- **Vulnerability Class:** Logical memory corruption during reassembly of fragmented IPsec ESP packets in `net/xfrm/xfrm_input.c`.
- **Trigger Path:** Unprivileged Android applications invoke the standard Android Java API `android.net.IpSecManager` to allocate security associations and send fragmented ESP packets.
- **Primitive Acquired:** Deterministic arbitrary write to cached file pages in the kernel page cache (Clean Page Overwrite).

#### 2. Key Advantages on X6871
- **No Memory Race:** 100% deterministic write primitive with zero risk of kernel panics or race failures.
- **Bypasses All Memory Defenses:** Completely circumvents KASLR, Clang CFI, Shadow Call Stack, and SMEP/SMAP because it operates purely on page-cache filesystem buffers.
- **Direct Exploitation Route:**
  - Injecting a prebuilt `kernelsu.ko` module by overwriting a dormant or accessible vendor library file, then invoking `init_module()`.
  - Overwriting an executable cached binary (such as a vendor daemon) to execute arbitrary commands.

---

### Vector C: KernelSU Late-Load & LKM Injection

#### 1. The Module Signing Breakthrough
A major security barrier on many enterprise and OEM Android devices is `CONFIG_MODULE_SIG=y` (cryptographic verification of `.ko` files).
- In the X6871 kernel, **`# CONFIG_MODULE_SIG is not set`**.
- This means the kernel accepts unsigned modules directly via the `init_module` / `finit_module` system calls.

#### 2. Kernel Module Interface (KMI) Symbol Requirements
To successfully load an LKM into the live X6871 kernel:
- **Vermagic Match:** The module ELF header must declare:
  ```text
  5.10.237-android12-9-00014-gf82f7360927e-ab14119954 SMP preempt mod_unload modversions aarch64
  ```
- **Modversions CRC Compatibility:**
  Because the X6871 kernel was built from Google's Android Common Kernel (ACK) `android12-5.10` branch (`ab14119954`), its exported symbol CRCs match Google's official GKI KMI symbol list.
  Any KernelSU module compiled against the `android12-5.10` ACK tree with matching vermagic can be loaded into memory without kernel panics.

---

## 4. Comparison with Speed Lock 5.10 Archive Projects

| Project | Target Device & SoC | Kernel Release | CFI / SCS Status | Module Signing | Exploit Method | Adaptability to X6871 |
|---|---|---|---|---|---|---|
| **GhostLock** (R0rt1z2) | Amazon Fire Max 11 (`MT8188J`) | Linux 5.10 vendor | CFI disabled on older builds | Unenforced | Futex UAF -> `miscdevice->fops` hijack -> `call_usermodehelper` | **High:** Proves futex exploitation on MediaTek ARM64 5.10 hardware. |
| **IonStack-S22U** | Galaxy S22+ (`SM-S906E`, SM8450) | `5.10.236` GKI 2.0 | **CFI + SCS Enabled** | Enforced (Samsung Knox) | Futex PI -> in-process `pselect` stamp -> UMH root helper | **Direct Match:** Exact same GKI 5.10 baseline, 39-bit VA, and CFI+SCS configuration. |
| **IonStackQuest3** | Meta Quest 3 (SM8550-VR) | `5.10.240` GKI 2.0 | CFI Enabled | Unenforced | Futex PI -> `mm_struct` order-2 reclaim -> `kimage_text_base` leak | **High:** Includes automated symbol extraction scripts (`gen_ionstack_config.py`). |
| **DirtyFrag-mitschud** | Generic Android GKI | Linux 5.10 - 6.12 | Bypasses CFI | N/A | xfrm-ESP page cache overwrite -> LKM injection | **High:** Ships prebuilt `dfroot-android12-5.10.ko` compatible with GKI 5.10. |
| **UniRoot** | Samsung Galaxy S22 - S26 | Linux 5.10 - 6.12 | Bypasses CFI | Overcomes Knox | Unprivileged app -> DirtyFrag / GhostLock -> KSU LKM | **High:** Provides complete user-facing APK deployment engine. |
| **ghostlock-app** | Infinix GT 30 (`X6876`), GT 30 Pro (`X6873`) | GKI 6.1 (MT6897) | CFI Enabled | Unenforced | Transsion profile format + Shizuku / APK deployment | **Direct Family:** Sister Transsion GT series; profile format directly portable. |

---

## 5. Technical Roadmap to Build an X6871 Target Profile

To implement a locked-bootloader root implementation for the Infinix GT 20 Pro (`X6871`), the following engineering steps are required:

### Step 1: Symbol Offset Extraction from `Image-x6871-5.10.237`
Using the extracted kernel binary (`research/firmware/X6871/kernel-metadata/Image-x6871-5.10.237`), extract core symbol offsets using `kallsyms` / `gen_ionstack_config.py`:
- `call_usermodehelper_exec_work` (Entry point for CFI-compliant usermode helper execution)
- `system_unbound_wq` (Target workqueue for background task execution)
- `init_cred` / `commit_creds` (Root credential assignment)
- `selinux_state` (SELinux permissive toggling)
- `kimage_voffset` / `_text` (KASLR slide calculation)

### Step 2: C Struct Member Offsets Resolution
Inspect GKI `android12-5.10` source code with Clang 12.0.5 and extracted `.config` settings to verify field offsets in:
- `struct task_struct` (offsets for `cred`, `real_cred`, `mm`, `files`)
- `struct work_struct` (offsets for `data`, `entry`, `func`)
- `struct futex_pi_state` and `struct rt_mutex_waiter`

### Step 3: Module Compilation for KernelSU Late-Load
1. Check out Google Android Common Kernel branch `android12-5.10-2025-09` (commit `f82f7360927e`).
2. Integrate KernelSU v0.9.x source tree.
3. Compile with Clang 12.0.5:
   ```bash
   make ARCH=arm64 LLVM=1 -C /path/to/ack_5.10 M=$(pwd)/KernelSU modules
   ```
4. Verify the generated `kernelsu.ko` matches the exact vermagic string:
   `5.10.237-android12-9-00014-gf82f7360927e-ab14119954 SMP preempt mod_unload modversions aarch64`

### Step 4: Profile Packaging for `ghostlock-app`
Create `x6871_5.10.237.json` adhering to the Transsion profile specification found in `ghostlock-app/docs/kernel_profiles/` and deploy via Shizuku or unprivileged APK.

---

## 6. Conclusion

The forensic investigation of the Infinix GT 20 Pro (`X6871`) stock firmware yields a decisive verdict:
- The device runs an **authentic Google GKI 2.0 kernel** (`5.10.237`), compiled in **September 2025**, with an AVB security patch date of **`2026-07-01`**.
- It is **definitively unpatched against both CVE-2026-43499 (IonStack / GhostLock) and CVE-2026-43284 (DirtyFrag)**.
- **Kernel module signing is disabled**, making runtime KernelSU LKM injection exceptionally viable.
- The architectural alignment with the Galaxy S22+ (`IonStack-S22U`) and Amazon Fire Max 11 (`GhostLock`) provides a clear, proven technical pathway for locked-bootloader temporary root.
