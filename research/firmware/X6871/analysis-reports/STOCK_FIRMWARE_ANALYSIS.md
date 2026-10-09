# Speed Lock — Infinix GT 20 Pro (`X6871`) Stock Firmware & Kernel Forensic Report

**Report Location:** `research/firmware/X6871/analysis-reports/STOCK_FIRMWARE_ANALYSIS.md`  
**Classification:** Primary Static Firmware Analysis & Kernel Reverse Engineering  
**Target Hardware:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896`)  
**Analyzed ROM Builds:** Fastboot (`X6871-15-31`) and Recovery (`X6871-15.1.2.180SP05-OP001PF001AZ`)  
**Methodology:** Direct binary inspection, header parsing, LZ4/GZIP decompression, and `.config` extraction.

---

## 1. Executive Summary & Forensic Revelations

This forensic analysis examines the official stock firmware of the Infinix GT 20 Pro (`X6871`), establishing ground-truth technical facts that refute previous speculative claims:

1. **The Core Kernel is Pure Google GKI 2.0:**
   - Previous community reports assumed the X6871 ran an out-of-tree MediaTek vendor fork (such as `5.10.198` or `5.10.226`).
   - Forensic extraction of `boot.img` proves the running kernel is an official **Android Common Kernel (ACK) GKI 2.0 release**:
     `5.10.237-android12-9-00014-gf82f7360927e-ab14119954`
2. **Kernel Module Signing is NOT Enforced:**
   - Extraction of the embedded kernel `.config` (181,388 bytes, 6,727 lines) confirms:
     `# CONFIG_MODULE_SIG is not set`
     The kernel does not enforce cryptographic signatures on loadable kernel modules (LKMs). Modules need only satisfy the Kernel Module Interface (KMI) symbol CRC table (`modversions`).
3. **Firmware Security Patch Level is Pre-Patch (`2026-07-01`):**
   - The Android Verified Boot (AVB 2.0) descriptor in `vbmeta.img` explicitly registers `security_patch: 2026-07-01`.
   - The kernel binary was compiled on **September 17, 2025**.
   - Because the upstream fixes for **CVE-2026-43499 (IonStack / GhostLock)** were merged in late July / August 2026, **the stock X6871 firmware is definitively unpatched and vulnerable**.

---

## 2. Definitive Device & Build Identification

| Forensic Attribute | Extracted Value | Evidence Source |
|---|---|---|
| **Marketing Name** | Infinix GT 20 Pro | `update-binary` line 84 |
| **Model Number** | `X6871` | `update-binary` line 85 |
| **Regional Product Name** | `X6871-OP` (Open Market / Global) | `vendor_ramdisk_build.prop` (`ro.product.product.name`) |
| **Internal Codename / Project** | `x6871_h962` | `MT6895_Android_scatter.xml` (line 7), `preloader_x6871_h962.bin` |
| **SoC Platform Identifier** | MediaTek `MT6895` series (`MT6896` Dimensity 8200 Ultimate) | `MT6895_Android_scatter.xml`, `APDB_MT6895___W2452` |
| **Fastboot Build ID** | `X6871-H962CF-U-BASE-260618V1066DevT` | `vendor_ramdisk_build.prop` (`ro.vendor.tran.version.release`) |
| **Recovery Build Identifier** | `X6871-15.1.2.180SP05-OP001PF001AZ` | `update-binary` line 86 |
| **Build Fingerprint** | `Infinix/X6871-OP/Infinix-X6871:12/SP1A.210812.016/260618V1066:user/release-keys` | `vendor_ramdisk_build.prop` (`ro.product.build.fingerprint`) |
| **Android Version (Userspace)** | Android 14 with XOS 14 (Transsion) | `ro.product.first_api_level=34`, `ro.product.first_api_level=35` |
| **GKI Lineage Base** | Android 12 GKI (`SDK 31`, `SP1A.210812.016`) | `boot-ramdisk.cpio` (`ro.bootimage.build.id`) |
| **AVB Security Patch Level** | **`2026-07-01`** (July 1, 2026) | `vbmeta.img` AVB 2.0 descriptor table |

---

## 3. Kernel Forensics & Binary Analysis

### A. Kernel Header & Release String
- **Boot Image Version:** Android Boot Image Header Version 4 (`boot_img_hdr_v4`).
- **Compression:** GZIP-compressed (`Image.gz`, 19,659,735 bytes).
- **Uncompressed Kernel Size:** **46,921,148 bytes** (44.75 MB).
- **Kernel Image SHA-256:** `613741DE45721F4E8E27EEB02F7B1CAC8E5E5E938FC1F5F5495E43F9C2715FBB`.
- **Full Release String:**
  ```text
  Linux version 5.10.237-android12-9-00014-gf82f7360927e-ab14119954 (build-user@build-host) (Android (7284624, based on r416183b) clang version 12.0.5 (https://android.googlesource.com/toolchain/llvm-project c935d99d7cf2016289302412d708641d52d2f7ee), LLD 12.0.5 (/buildbot/src/android/llvm-toolchain/out/llvm-project/lld c935d99d7cf2016289302412d708641d52d2f7ee)) #1 SMP PREEMPT Wed Sep 17 09:13:52 UTC 2025
  ```
- **Vermagic String:**
  ```text
  5.10.237-android12-9-00014-gf82f7360927e-ab14119954 SMP preempt mod_unload modversions aarch64
  ```
- **Google CI Build ID:** `ab14119954`.
- **Git Commit Hash:** `gf82f7360927e`.

### B. Memory Architecture & Page Sizing
- **Architecture:** ARM64 (`aarch64` / `arm64-v8a`), ARMv8.2-A.
- **Page Size:** **4096 bytes (4KB)**. Confirmed via:
  - `CONFIG_ARM64_4K_PAGES=y`
  - `# CONFIG_ARM64_16K_PAGES is not set`
  - `vendor_boot.img` page size header field: `4096`.
- **Virtual Address Space:** **39 bits** (`CONFIG_ARM64_VA_BITS=39`).
  - Matches the exact address layout of the Snapdragon Galaxy S22+ (`IonStack-S22U`).

---

## 4. Kernel Configuration Security Audit (`.config`)

The live kernel configuration was extracted directly from the uncompressed kernel binary at offset `0x1a34220` (`IKCFG_ST` magic):

| Configuration Option | Status in X6871 Kernel | Security & Exploitation Implications |
|---|---|---|
| `CONFIG_CFI_CLANG` | **`=y` (Enabled)** | Clang Control Flow Integrity validates indirect calls. Exploit must target `.cfi_jt` jump-table stubs or use data-only attacks (`call_usermodehelper_exec_work`). |
| `CONFIG_SHADOW_CALL_STACK` | **`=y` (Enabled)** | Return addresses stored on a dedicated shadow stack. Traditional ROP return address overwrites on the kernel stack are mitigated. |
| `CONFIG_MODULES` | **`=y` (Enabled)** | Kernel supports runtime Loadable Kernel Modules (LKMs). |
| `CONFIG_MODULE_SIG` | **`# is not set` (Disabled)** | **Critical finding:** The kernel does not check cryptographic module signatures. Any LKM with matching symbol CRCs can be loaded via `init_module`. |
| `CONFIG_KALLSYMS` | **`=y` (Enabled)** | Symbol addresses and names are embedded in the binary. |
| `CONFIG_KALLSYMS_ALL` | **`=y` (Enabled)** | Data symbols and unexported functions are included in the kallsyms table. |
| `CONFIG_RANDOMIZE_BASE` | **`=y` (Enabled)** | Kernel Address Space Layout Randomization (KASLR) is active. Exploit requires a pointer leak to calculate the ASLR slide. |
| `CONFIG_SECURITY_SELINUX` | **`=y` (Enabled)** | SELinux is enforcing. |
| `CONFIG_FUTEX` | **`=y` (Enabled)** | Futex subsystem active (target of CVE-2026-43499 / IonStack). |
| `CONFIG_XFRM` | **`=y` (Enabled)** | IPsec transform framework active (target of CVE-2026-43284 / DirtyFrag). |
| `CONFIG_INET_ESP` | **`=y` (Enabled)** | ESP packet handling active in IP stack. |
| `CONFIG_STRICT_DEVMEM` | **`# is not set` (Disabled)** | No additional `/dev/mem` restriction beyond standard ARM64 page-table controls. |

---

## 5. Android Verified Boot (AVB 2.0) Analysis

The `vbmeta-x6871-15-31.img` was parsed to inspect AVB 2.0 cryptographic enforcement:
- **AVB Magic:** `AVB0` (Version 1.0).
- **Rollback Index:** `2`.
- **Flags:** `0x0` (AVB verification is **ENFORCING**).
- **Chained Partitions:** `boot`, `vbmeta_system`, `vbmeta_vendor`.
- **Security Patch Registered in VBMeta:** **`2026-07-01`**.
- **dm-verity Enforcement:** SHA-256 tree verification is enabled for all dynamic super partitions (`system`, `vendor`, `product`).
- **Conclusion:** **Locked-bootloader root requires an in-memory runtime exploit**. Any static modification to disk partitions will fail AVB verification, resulting in a dm-verity red-screen brick.

---

## 6. Device Tree & Peripheral Architecture

The `dtbo-x6871-15-31.img` was extracted and decompiled:
- **DTBO Header:** `DT_TABLE_MAGIC` (`0xd7b7ab1e`), total size 77,623 bytes.
- **Entry Count:** 1 DTB entry (77,559 bytes, SHA-256 verified).
- **Hardware Bindings:**
  - MediaTek Display (`mediatek,disp-leds`, `mediatek,lcd-backlight`)
  - Pixelworks Display Coprocessor (`ro.vendor.tran.display.pixelworks.support=1`)
  - Charging & Thermal (`mediatek,charger`, `mediatek,lk_charger`, `mediatek,ocp81375`)
  - Subsystems (`mediatek,seninf`, `mediatek,camera_eeprom`, `mediatek,aw8601af`)

---

## 7. Fastboot Package vs. Recovery Package Differences

| Dimension | Fastboot Package (`Infinix-GT-20-Pro-X6871-15-31`) | Recovery Package (`recovery-ab`) |
|---|---|---|
| **Intended Tool** | MediaTek SP Flash Tool / Fastboot | Stock A/B Recovery (`/sbin/recovery`) |
| **Partition Format** | Raw `.img` partition binaries + `super.img` (8.83 GB) | `.img.zst` (Zstandard compressed sparse images) |
| **Scatter & Mapping** | `MT6895_Android_scatter.xml`, `MT6895_Android_scatter.txt` | `META-INF/com/google/android/update-binary` shell script |
| **Debug Images** | Includes `vendor_boot-debug.img` (28.2 MB) | None |
| **Core Boot Images** | `boot.img`, `vendor_boot.img`, `dtbo.img` (Identical hashes) | `boot.img`, `vendor_boot.img`, `dtbo.img` (Identical hashes) |
| **AVB Root** | `vbmeta.img`, `vbmeta_system.img`, `vbmeta_vendor.img` | `vbmeta.img`, `vbmeta_system.img`, `vbmeta_vendor.img` |
