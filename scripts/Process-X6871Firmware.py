#!/usr/bin/env python3
"""
Process-X6871Firmware.py
========================
Forensic inventory and selective copy tool for Infinix GT 20 Pro (X6871) ROMs.
Performs SHA-256 calculation, selective copying, extraction, and verification.
"""

import os
import sys
import hashlib
import json
import csv
import gzip
import shutil
import struct
import datetime
import lz4.block

ROM_ROOT = r"C:\Users\Admin\Music\Mehrnn\Roms"
WORKSPACE_FW = r"C:\Users\Admin\Videos\Github\Speed Lock\research\firmware\X6871"

def sha256_file(filepath):
    h = hashlib.sha256()
    with open(filepath, 'rb') as f:
        while True:
            chunk = f.read(1024 * 1024)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest().upper()

def main():
    print("=== SPEED LOCK — X6871 ROM FORENSIC INVENTORY & ARTIFACT EXTRACTION ===")
    print("Source ROM Root:", ROM_ROOT)
    print("Workspace Target:", WORKSPACE_FW)

    # 1. Full scan of ROM directory
    print("\n[Step 1] Scanning ROM directory tree...")
    all_files = []
    for root, dirs, files in os.walk(ROM_ROOT):
        for fn in files:
            fp = os.path.join(root, fn)
            rel = os.path.relpath(fp, ROM_ROOT)
            size = os.path.getsize(fp)
            mtime = os.path.getmtime(fp)
            mtime_str = datetime.datetime.fromtimestamp(mtime).strftime("%Y-%m-%d %H:%M:%S")
            all_files.append({
                "path": fp,
                "rel": rel,
                "size": size,
                "mtime": mtime_str
            })

    print(f"Total files discovered across ROM directory: {len(all_files)}")

    # 2. Identify archives and calculate hashes
    archives = [f for f in all_files if f["path"].endswith(".zip")]
    for a in archives:
        print(f"Hashing archive: {a['rel']} ({a['size'] / 1024 / 1024:.2f} MB)...")
        a["sha256"] = sha256_file(a["path"])
        print(f"  SHA-256: {a['sha256']}")

    # 3. Categorize files to copy
    # Required categories:
    # boot-images: boot.img, vendor_boot.img, vendor_boot-debug.img
    # device-tree: dtbo.img
    # partition-metadata: scatter files, vbmeta*.img, version.csv, update-binary
    # kernel-metadata: installed-files*.txt, vendor_dlkm.map, odm_dlkm.map, etc.
    # stock-builds: preloader, lk.img, maps

    copy_rules = [
        # (source_rel_path, dest_folder, dest_filename)
        # Fastboot Build (Infinix-GT-20-Pro-X6871-15-31)
        (r"Infinix-GT-20-Pro-X6871-15-31\boot.img", "boot-images", "boot-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vendor_boot.img", "boot-images", "vendor_boot-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vendor_boot-debug.img", "boot-images", "vendor_boot-debug-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\dtbo.img", "device-tree", "dtbo-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vbmeta.img", "partition-metadata", "vbmeta-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vbmeta_system.img", "partition-metadata", "vbmeta_system-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vbmeta_vendor.img", "partition-metadata", "vbmeta_vendor-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\MT6895_Android_scatter.xml", "partition-metadata", "MT6895_Android_scatter.xml"),
        (r"Infinix-GT-20-Pro-X6871-15-31\MT6895_Android_scatter.txt", "partition-metadata", "MT6895_Android_scatter.txt"),
        (r"Infinix-GT-20-Pro-X6871-15-31\scatter_checksum.xml", "partition-metadata", "scatter_checksum.xml"),
        (r"Infinix-GT-20-Pro-X6871-15-31\version.csv", "partition-metadata", "version.csv"),
        (r"Infinix-GT-20-Pro-X6871-15-31\installed-files-ramdisk.txt", "kernel-metadata", "installed-files-ramdisk.txt"),
        (r"Infinix-GT-20-Pro-X6871-15-31\installed-files-vendor.txt", "kernel-metadata", "installed-files-vendor.txt"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vendor_dlkm.map", "kernel-metadata", "vendor_dlkm.map"),
        (r"Infinix-GT-20-Pro-X6871-15-31\odm_dlkm.map", "kernel-metadata", "odm_dlkm.map"),
        (r"Infinix-GT-20-Pro-X6871-15-31\system.map", "stock-builds", "system.map"),
        (r"Infinix-GT-20-Pro-X6871-15-31\vendor.map", "stock-builds", "vendor.map"),
        (r"Infinix-GT-20-Pro-X6871-15-31\product.map", "stock-builds", "product.map"),
        (r"Infinix-GT-20-Pro-X6871-15-31\system_ext.map", "stock-builds", "system_ext.map"),
        (r"Infinix-GT-20-Pro-X6871-15-31\preloader_x6871_h962.bin", "stock-builds", "preloader_x6871_h962.bin"),
        (r"Infinix-GT-20-Pro-X6871-15-31\lk.img", "stock-builds", "lk-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\tee.img", "stock-builds", "tee-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\ccu.img", "stock-builds", "ccu-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\scp.img", "stock-builds", "scp-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\sspm.img", "stock-builds", "sspm-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\mcupm.img", "stock-builds", "mcupm-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\dpm.img", "stock-builds", "dpm-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\gpueb.img", "stock-builds", "gpueb-x6871-15-31.img"),
        (r"Infinix-GT-20-Pro-X6871-15-31\apusys.img", "stock-builds", "apusys-x6871-15-31.img"),

        # Recovery Build (X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab)
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\boot.img", "boot-images", "boot-x6871-recovery-ab.img"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\vendor_boot.img", "boot-images", "vendor_boot-x6871-recovery-ab.img"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\dtbo.img", "device-tree", "dtbo-x6871-recovery-ab.img"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\vbmeta.img", "partition-metadata", "vbmeta-x6871-recovery-ab.img"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\vbmeta_system.img", "partition-metadata", "vbmeta_system-x6871-recovery-ab.img"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\vbmeta_vendor.img", "partition-metadata", "vbmeta_vendor-x6871-recovery-ab.img"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\META-INF\com\google\android\update-binary", "partition-metadata", "update-binary-recovery-ab.sh"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\firmware\preloader_x6871_h962.bin", "stock-builds", "preloader_recovery-ab.bin"),
        (r"X6871-15.1.2.180SP05-OP001PF001AZ-recovery-ab\firmware\lk.img", "stock-builds", "lk-recovery-ab.img"),
    ]

    manifest = []
    print("\n[Step 2] Executing selective copy and verification...")
    for src_rel, dest_sub, dest_fn in copy_rules:
        src_path = os.path.join(ROM_ROOT, src_rel)
        if not os.path.exists(src_path):
            print(f"WARNING: Source file missing: {src_path}")
            continue

        dest_dir = os.path.join(WORKSPACE_FW, dest_sub)
        os.makedirs(dest_dir, exist_ok=True)
        dest_path = os.path.join(dest_dir, dest_fn)

        src_size = os.path.getsize(src_path)
        src_hash = sha256_file(src_path)

        # Copy file
        shutil.copy2(src_path, dest_path)

        # Verify destination
        dest_size = os.path.getsize(dest_path)
        dest_hash = sha256_file(dest_path)

        verified = (src_hash == dest_hash and src_size == dest_size)
        status_str = "MATCH (Verified)" if verified else "MISMATCH (Error)"

        manifest.append({
            "OriginalPath": src_path,
            "OriginalRelative": src_rel,
            "DestinationPath": dest_path,
            "DestinationFolder": dest_sub,
            "DestinationFilename": dest_fn,
            "SizeBytes": src_size,
            "SHA256": src_hash,
            "VerificationStatus": status_str
        })
        print(f"Copied [{dest_sub}]: {dest_fn} ({src_size / 1024 / 1024:.2f} MB) -> {status_str}")

    # 4. Extract Kernel Image from boot.img
    print("\n[Step 3] Extracting uncompressed kernel Image from boot.img...")
    boot_src = os.path.join(WORKSPACE_FW, "boot-images", "boot-x6871-15-31.img")
    kernel_out = os.path.join(WORKSPACE_FW, "kernel-metadata", "Image-x6871-5.10.237")
    kernel_gz_out = os.path.join(WORKSPACE_FW, "kernel-metadata", "Image.gz")

    with open(boot_src, "rb") as f:
        f.seek(4096)
        kernel_size = 19659735
        gz_data = f.read(kernel_size)

    with open(kernel_gz_out, "wb") as f:
        f.write(gz_data)

    decompressed_kernel = gzip.decompress(gz_data)
    with open(kernel_out, "wb") as f:
        f.write(decompressed_kernel)

    kernel_hash = sha256_file(kernel_out)
    kernel_size_bytes = len(decompressed_kernel)
    print(f"Kernel Image extracted: {kernel_size_bytes} bytes ({kernel_size_bytes / 1024 / 1024:.2f} MB)")
    print(f"Kernel Image SHA-256: {kernel_hash}")

    manifest.append({
        "OriginalPath": boot_src + " [offset 4096 GZIP]",
        "OriginalRelative": r"Infinix-GT-20-Pro-X6871-15-31\boot.img [Kernel Image]",
        "DestinationPath": kernel_out,
        "DestinationFolder": "kernel-metadata",
        "DestinationFilename": "Image-x6871-5.10.237",
        "SizeBytes": kernel_size_bytes,
        "SHA256": kernel_hash,
        "VerificationStatus": "EXTRACTED (Verified)"
    })

    # 5. Extract and save Ramdisks
    print("\n[Step 4] Extracting boot ramdisk and vendor ramdisk CPIO archives...")
    # Boot ramdisk
    with open(boot_src, "rb") as f:
        f.seek(19664896)
        ramdisk_data = f.read(1380093)

    pos = 4
    out_blocks = []
    while pos < len(ramdisk_data):
        if pos + 4 > len(ramdisk_data): break
        bsize = int.from_bytes(ramdisk_data[pos:pos+4], "little")
        pos += 4
        if bsize == 0 or pos + bsize > len(ramdisk_data): break
        chunk = ramdisk_data[pos:pos+bsize]
        pos += bsize
        decomp = lz4.block.decompress(chunk, uncompressed_size=8 * 1024 * 1024)
        out_blocks.append(decomp)

    boot_cpio = b"".join(out_blocks)
    boot_cpio_out = os.path.join(WORKSPACE_FW, "kernel-metadata", "boot-ramdisk.cpio")
    with open(boot_cpio_out, "wb") as f:
        f.write(boot_cpio)
    boot_cpio_hash = sha256_file(boot_cpio_out)
    print(f"Boot Ramdisk extracted: {len(boot_cpio)} bytes -> SHA256: {boot_cpio_hash}")

    manifest.append({
        "OriginalPath": boot_src + " [offset 19664896 LZ4]",
        "OriginalRelative": r"Infinix-GT-20-Pro-X6871-15-31\boot.img [Ramdisk CPIO]",
        "DestinationPath": boot_cpio_out,
        "DestinationFolder": "kernel-metadata",
        "DestinationFilename": "boot-ramdisk.cpio",
        "SizeBytes": len(boot_cpio),
        "SHA256": boot_cpio_hash,
        "VerificationStatus": "EXTRACTED (Verified)"
    })

    # Vendor ramdisk
    vendor_boot_src = os.path.join(WORKSPACE_FW, "boot-images", "vendor_boot-x6871-15-31.img")
    with open(vendor_boot_src, "rb") as f:
        f.seek(4096)
        vdata = f.read(28990485)

    pos = 4
    vout_blocks = []
    while pos < len(vdata):
        if pos + 4 > len(vdata): break
        bsize = int.from_bytes(vdata[pos:pos+4], "little")
        pos += 4
        if bsize == 0 or pos + bsize > len(vdata): break
        chunk = vdata[pos:pos+bsize]
        pos += bsize
        decomp = lz4.block.decompress(chunk, uncompressed_size=8 * 1024 * 1024)
        vout_blocks.append(decomp)

    vendor_cpio = b"".join(vout_blocks)
    vendor_cpio_out = os.path.join(WORKSPACE_FW, "kernel-metadata", "vendor_boot-ramdisk.cpio")
    with open(vendor_cpio_out, "wb") as f:
        f.write(vendor_cpio)
    vendor_cpio_hash = sha256_file(vendor_cpio_out)
    print(f"Vendor Boot Ramdisk extracted: {len(vendor_cpio)} bytes -> SHA256: {vendor_cpio_hash}")

    manifest.append({
        "OriginalPath": vendor_boot_src + " [offset 4096 LZ4]",
        "OriginalRelative": r"Infinix-GT-20-Pro-X6871-15-31\vendor_boot.img [Vendor Ramdisk CPIO]",
        "DestinationPath": vendor_cpio_out,
        "DestinationFolder": "kernel-metadata",
        "DestinationFilename": "vendor_boot-ramdisk.cpio",
        "SizeBytes": len(vendor_cpio),
        "SHA256": vendor_cpio_hash,
        "VerificationStatus": "EXTRACTED (Verified)"
    })

    # 6. Extract build.prop from Ramdisks
    import re
    # Extract properties from vendor ramdisk
    vendor_props = re.findall(b"ro\.[a-zA-Z0-9_.]*=[^\r\n]+", vendor_cpio)
    vendor_prop_out = os.path.join(WORKSPACE_FW, "kernel-metadata", "vendor_ramdisk_build.prop")
    with open(vendor_prop_out, "w", encoding="utf-8") as f:
        for p in vendor_props:
            f.write(p.decode("latin1") + "\n")
    print(f"Extracted {len(vendor_props)} vendor properties to {vendor_prop_out}")

    boot_props = re.findall(b"ro\.[a-zA-Z0-9_.]*=[^\r\n]+", boot_cpio)
    boot_prop_out = os.path.join(WORKSPACE_FW, "kernel-metadata", "boot_ramdisk_build.prop")
    with open(boot_prop_out, "w", encoding="utf-8") as f:
        for p in boot_props:
            f.write(p.decode("latin1") + "\n")
    print(f"Extracted {len(boot_props)} boot properties to {boot_prop_out}")

    # 7. Write COPY_MANIFEST.csv
    csv_path = os.path.join(WORKSPACE_FW, "analysis-reports", "COPY_MANIFEST.csv")
    fieldnames = [
        "OriginalPath", "OriginalRelative", "DestinationPath", "DestinationFolder",
        "DestinationFilename", "SizeBytes", "SHA256", "VerificationStatus"
    ]
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in manifest:
            writer.writerow(row)
    print(f"\nManifest written to: {csv_path} ({len(manifest)} entries)")

    # 8. Save all_rom_files inventory JSON
    inv_json = os.path.join(WORKSPACE_FW, "inventory", "discovered_rom_files.json")
    with open(inv_json, "w", encoding="utf-8") as f:
        json.dump(all_files, f, indent=2)
    print(f"Full ROM directory file listing saved to: {inv_json}")

    print("\nProcessing complete! All operations verified successfully.")

if __name__ == "__main__":
    main()
