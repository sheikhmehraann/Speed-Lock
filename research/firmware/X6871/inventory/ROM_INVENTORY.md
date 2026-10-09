# Speed Lock — Infinix GT 20 Pro (`X6871`) Stock ROM Inventory

**Inventory Location:** `research/firmware/X6871/inventory/ROM_INVENTORY.md`  
**Source Directory:** `C:\Users\Admin\Music\Mehrnn\Roms`  
**Workspace Destination:** `C:\Users\Admin\Videos\Github\Speed Lock\research\firmware\X6871\`  
**Date of Audit:** 2026-10-09  
**Total Source Files Scanned:** 123  
**Integrity Rule:** All source ROMs preserved without modification or deletion.

---

## 1. Discovered ROM Archives & Firmware Packages

| Package Name | Type | Size (Bytes) | Size (MB / GB) | SHA-256 Checksum | Modification Date | Discovered State |
|---|---|---|---|---|---|---|
| **`Infinix-GT-20-Pro-X6871-15-31.zip`** | Fastboot / SP Flash Tool Archive | 8,461,051,956 | 8,069.09 MB (~8.46 GB) | `DDCD785B15872A8C98307797F2DF4853D4039637733AA79CA12002C9637B9544` | 2026-09-23 02:03:34 | Complete ZIP Archive |
| **`Infinix-GT-20-Pro-X6871-15-31/`** | Fastboot / SP Flash Tool (Extracted) | 9,634,818,487 | ~9.63 GB (65 files) | N/A (Directory) | 2026-09-23 02:18:21 | Extracted Partition Images |
| **`X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab.zip`** | Recovery A/B OTA Archive | 8,268,135,546 | 7,885.11 MB (~8.27 GB) | `C6185F0DEBFF07E3709B3208D75C6B04B94214A67ADA25713081B472739C26D4` | 2026-09-20 14:24:39 | Complete ZIP Archive |
| **`X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab/`** | Recovery A/B OTA (Extracted) | 8,432,192,841 | ~8.43 GB (56 files) | N/A (Directory) | 2026-09-20 14:39:05 | Extracted Recovery Files |

---

## 2. Cross-Build Content Hash Comparison (Duplicate Detection)

Comparing SHA-256 hashes of critical partition images across both extracted packages confirms whether the underlying kernel and boot components are identical:

| Partition / Image File | Fastboot Build (`15-31`) SHA-256 | Recovery Build (`recovery-ab`) SHA-256 | Match Status | Forensic Meaning |
|---|---|---|---|---|
| **`boot.img`** | `1A5A823B6F05E04304240255D8CD92572118122FA86BA4FB254A919F44B806A6` | `1A5A823B6F05E04304240255D8CD92572118122FA86BA4FB254A919F44B806A6` | **100% IDENTICAL** | Both packages deploy the exact same core GKI 5.10 kernel |
| **`vendor_boot.img`** | `0966CF258584D5ED31C8E734427250936DC5671D1D2EED8FC2985532B50FECCD` | `0966CF258584D5ED31C8E734427250936DC5671D1D2EED8FC2985532B50FECCD` | **100% IDENTICAL** | Vendor ramdisk and boot parameters are identical |
| **`dtbo.img`** | `7F25930B23BD783CAFC6959576BD86F56F1ECDFF707A628EB3DD4061A7CD33A3` | `7F25930B23BD783CAFC6959576BD86F56F1ECDFF707A628EB3DD4061A7CD33A3` | **100% IDENTICAL** | Device tree overlays match bit-for-bit |
| **`vbmeta.img`** | `65FE5184B190FD38DC59E15525BE1DC3897C078DC4C8C198FA76B55E2B99EFA9` | `65FE5184B190FD38DC59E15525BE1DC3897C078DC4C8C198FA76B55E2B99EFA9` | **100% IDENTICAL** | Android Verified Boot root descriptors match |
| **`vbmeta_system.img`** | `8BD992857C76A7A407FA25C6BFA47F52E58E297A48E4B07B656F623631620E32` | `8BD992857C76A7A407FA25C6BFA47F52E58E297A48E4B07B656F623631620E32` | **100% IDENTICAL** | System partition AVB tree matches |
| **`vbmeta_vendor.img`** | `89C82A2E8B9287CFE3F40E92929BA369DBEBC7262DDF71C355C925B4467E0A83` | `89C82A2E8B9287CFE3F40E92929BA369DBEBC7262DDF71C355C925B4467E0A83` | **100% IDENTICAL** | Vendor partition AVB tree matches |
| **`lk.img`** | `14F2CD10AE825FE6994119CE59FF273570F28400492AC170EBFE430CC411F49C` | `14F2CD10AE825FE6994119CE59FF273570F28400492AC170EBFE430CC411F49C` | **100% IDENTICAL** | Little Kernel (bootloader second stage) is identical |

---

## 3. Selective Copying & Archive Organization Strategy

To adhere strictly to the Karpathy guidelines of surgical simplicity and avoid consuming 25+ GB of unnecessary disk space with massive raw filesystem images, the workspace captures only the forensically essential components:

```text
Speed Lock/
└── research/
    └── firmware/
        └── X6871/
            ├── inventory/
            │   ├── ROM_INVENTORY.md              <-- This comprehensive inventory
            │   └── discovered_rom_files.json     <-- 123-entry raw file catalogue
            ├── boot-images/
            │   ├── boot-x6871-15-31.img          <-- Primary GKI boot image (64 MB)
            │   ├── boot-x6871-recovery-ab.img    <-- Recovery boot image duplicate (64 MB)
            │   ├── vendor_boot-x6871-15-31.img   <-- Vendor boot with MTK ramdisk (64 MB)
            │   ├── vendor_boot-x6871-recovery.img<-- Recovery vendor boot (64 MB)
            │   └── vendor_boot-debug-x6871-15-31.img <-- Debug vendor boot variant (28.2 MB)
            ├── kernel-metadata/
            │   ├── Image-x6871-5.10.237          <-- Decompressed kernel binary (44.75 MB)
            │   ├── Image.gz                      <-- Extracted compressed kernel (18.75 MB)
            │   ├── boot-ramdisk.cpio             <-- Decompressed GKI ramdisk (2.41 MB)
            │   ├── vendor_boot-ramdisk.cpio      <-- Decompressed vendor ramdisk (67.45 MB)
            │   ├── boot_ramdisk_build.prop       <-- 15 extracted GKI boot properties
            │   ├── vendor_ramdisk_build.prop     <-- 637 extracted Transsion vendor properties
            │   ├── extracted_config.txt          <-- Full live kernel .config (181 KB, 6727 lines)
            │   ├── vendor_dlkm.map               <-- LKM module layout map
            │   ├── odm_dlkm.map                  <-- ODM module map
            │   ├── installed-files-ramdisk.txt   <-- Ramdisk file list
            │   └── installed-files-vendor.txt    <-- Complete vendor partition file list
            ├── device-tree/
            │   ├── dtbo-x6871-15-31.img          <-- Raw DTBO table image (8 MB)
            │   ├── dtbo-x6871-recovery-ab.img    <-- Recovery DTBO image (8 MB)
            │   └── dtb_extracted/
            │       └── dtb_0_id_0x0.dtb          <-- Extracted Device Tree Blob (77.5 KB)
            ├── partition-metadata/
            │   ├── MT6895_Android_scatter.xml    <-- MediaTek XML partition table (122 KB)
            │   ├── MT6895_Android_scatter.txt    <-- MediaTek text scatter file (75 KB)
            │   ├── scatter_checksum.xml          <-- Checksum descriptor
            │   ├── version.csv                   <-- ROM version definition
            │   ├── update-binary-recovery-ab.sh  <-- Recovery flashing script
            │   ├── vbmeta-x6871-15-31.img        <-- AVB 2.0 root descriptor (12 KB)
            │   ├── vbmeta_system-x6871-15-31.img <-- System AVB descriptor (4 KB)
            │   └── vbmeta_vendor-x6871-15-31.img <-- Vendor AVB descriptor (4 KB)
            ├── stock-builds/
            │   ├── preloader_x6871_h962.bin      <-- MediaTek Stage-1 Preloader (502 KB)
            │   ├── lk-x6871-15-31.img            <-- Little Kernel Stage-2 (2.57 MB)
            │   ├── tee-x6871-15-31.img           <-- TrustZone Secure OS (860 KB)
            │   ├── ccu.img, scp.img, sspm.img    <-- Subsystem firmware images
            │   └── system.map, vendor.map, etc.  <-- Block mapping descriptors
            └── analysis-reports/
                ├── STOCK_FIRMWARE_ANALYSIS.md    <-- Detailed forensic technical report
                ├── KERNEL_5_10_COMPATIBILITY.md  <-- Compatibility and exploit lead matrix
                └── COPY_MANIFEST.csv             <-- 40-row verifiable file copy manifest
```

---

## 4. Omitted Large Files Justification

The following files were intentionally **not** duplicated into the workspace:

| Omitted File | Path in Source | Size | Technical Justification for Omission |
|---|---|---|---|
| `super.img` | `Infinix-GT-20-Pro-X6871-15-31\super.img` | 8.83 GB | Contains dynamic userspace filesystems (`system`, `vendor`, `product`). Kernel research focuses on boot partitions (`boot.img`, `vendor_boot.img`), not userspace application files. |
| `product.img.zst` | `recovery-ab\product.img.zst` | 2.80 GB | Compressed userspace product apps. |
| `system_ext.img.zst` | `recovery-ab\system_ext.img.zst` | 2.29 GB | Compressed system extension framework. |
| `vendor.img.zst` | `recovery-ab\vendor.img.zst` | 1.59 GB | Compressed vendor userspace HALs. |
| `system.img.zst` | `recovery-ab\system.img.zst` | 655 MB | Compressed Android framework. |
| `md1img.img` | `15-31\md1img.img` | 68 MB | Cellular baseband modem firmware image (not kernel space). |

Total disk space saved by selective copying: **~24.5 GB**.
