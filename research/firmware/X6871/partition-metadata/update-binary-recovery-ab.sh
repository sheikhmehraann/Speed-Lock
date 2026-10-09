#!/sbin/sh

OUTFD=/proc/self/fd/$2
ZIPFILE="$3"

ui_print() {
    printf 'ui_print %s\nui_print\n' "$1" >>"$OUTFD"
}

# Function to extract and flash a file to a partition
flash_partition() {
    src="$1"
    dest="$2"
    msg="$3"

    if [ "$#" -lt 3 ]; then
        partition_name=$(echo "$dest" | cut -d '/' -f 5)
        ui_print "- Flashing partition $partition_name"
    elif [ -n "$msg" ]; then
        ui_print "$msg"
    fi

    unzip -p "$ZIPFILE" "$src" >"$dest" || {
        ui_print "Error: Failed to flash $src to $dest"
        exit 1
    }
}

# Function to extract and flash a zstd compressed file to a partition
flash_partition_zstd() {
    src="$1"
    dest="$2"
    partition_name=$(echo "$dest" | cut -d '/' -f 5)

    ui_print "- Flashing partition $partition_name"
    unzip -p "$ZIPFILE" "$src" | /tmp/META-INF/zstd -c -d >"$dest" || {
        ui_print "Error: Failed to flash compressed $src to $dest"
        exit 1
    }
}

# Function to flash firmware to both slots while logging once
flash_firmware_both_slots() {
    img_file="$1"
    base_name="$2"

    flash_partition "$img_file" "/dev/block/by-name/${base_name}_a" "- Flashing partition ${base_name} to both slots"
    flash_partition "$img_file" "/dev/block/by-name/${base_name}_b" ""
}

getVolumeKey() {
    ui_print "- Listening to volume keys. Press [+] for 'Yes' and [-] for 'No'"
    while true; do
        keyInfo=$(getevent -qlc 1 | grep KEY_VOLUME)
        [ -z "$keyInfo" ] && continue
        isUpKey=$(printf '%s\n' "$keyInfo" | grep KEY_VOLUMEUP)
        if [ -n "$isUpKey" ]; then
            return 0
        else
            return 1
        fi
    done
}

checkDevice() {
    myDevice=$(getprop ro.product.device)
    [ -z "$myDevice" ] && myDevice=$(getprop ro.build.product)
    [ -z "$myDevice" ] && myDevice=$(getprop ro.product.name)
    romDevice="X6871"
    if [ -z "$(echo "$myDevice" | grep -i "$romDevice")" ]; then
        ui_print "- Device code verification failed. Please double-check if this package matches your device model."
        ui_print "- Flashing the wrong package may cause bricking, and you will bear the consequences. Do you want to continue flashing?"
        if ! getVolumeKey; then
            ui_print "- You chose to abort flashing."
            exit 1
        else
            ui_print "- You chose to continue flashing."
        fi
    fi
}

checkExit() {
    status=$?
    if [ "$status" -ne 0 ]; then
        ui_print "Error: Exit status $status detected. There may be an issue with your super partition. Flash the stock super.img first, then retry. Exiting..."
        exit 1
    fi
}

unmountPartitions() {
    umount /system /system_root /vendor /product /system_ext /vendor_dlkm /odm_dlkm /odm \
           /tr_carrier /tr_company /tr_mi /tr_preload /tr_product /tr_region /tr_theme \
           /tr_manifest /tr_misc 2>/dev/null
}

# Function to handle logical partition operations
manage_logical_partition() {
    operation="$1"
    partition="$2"
    size="$3"
    slot="$4"

    case "$operation" in
        clear)
            lptools unmap "$partition$slot" && lptools remove "$partition$slot"
            ;;
        create)
            lptools create "$partition$slot" "$size" || checkExit
            ;;
        create_optional)
            lptools create "$partition$slot" "$size" || true
            ;;
        map)
            lptools map "$partition$slot" || checkExit
            ;;
        unmap_map)
            lptools unmap "$partition$slot"
            lptools map "$partition$slot" || checkExit
            ;;
    esac
}

create_partitions_for_slot() {
    target_slot="$1"
    other_slot="$2"
    shift 2

    for spec in "$@"; do
        partition="${spec%%:*}"
        size="${spec#*:}"
        manage_logical_partition "create" "$partition" "$size" "$target_slot"
        manage_logical_partition "create_optional" "$partition" "0" "$other_slot"
    done
}

process_partitions_for_slots() {
    operation="$1"
    shift

    for partition in "$@"; do
        manage_logical_partition "$operation" "$partition" "" "_a"
        manage_logical_partition "$operation" "$partition" "" "_b"
    done
}

process_partitions_for_slot() {
    operation="$1"
    slot="$2"
    shift 2

    for partition in "$@"; do
        manage_logical_partition "$operation" "$partition" "" "$slot"
    done
}

unzip -o "$ZIPFILE" META-INF/zstd -d /tmp
chmod 0755 /tmp/META-INF/zstd

ui_print " "
ui_print "============================================"
ui_print "██████     ██████     ██████  "
ui_print "██   ██    ██   ██    ██   ██ "
ui_print "██████     ██████     ██████  "
ui_print "██   ██    ██   ██    ██      "
ui_print "██   ██ ██ ██████  ██ ██      "
ui_print " "
ui_print "Flashable ROM by ramabondanp"
ui_print "Device : Infinix GT 20 Pro"
ui_print "Version: X6871-15.1.2.180SP05-OP001PF001AZ"
ui_print "============================================"

checkDevice

unmountPartitions

ui_print " "
SLOT=$(getprop ro.boot.slot_suffix)
ui_print "Checking boot slot... ${SLOT}"

# Remap
lptools clear-cow
checkExit

# Firmware
ui_print " "
ui_print "Patching firmware to both slot..."
flash_firmware_both_slots "firmware/apusys.img" "apusys"
flash_firmware_both_slots "firmware/ccu.img" "ccu"
flash_firmware_both_slots "firmware/dpm.img" "dpm"
flash_firmware_both_slots "firmware/gpueb.img" "gpueb"
flash_firmware_both_slots "firmware/gz.img" "gz"
flash_firmware_both_slots "firmware/lk.img" "lk"
flash_firmware_both_slots "firmware/logo.img" "logo"
flash_firmware_both_slots "firmware/mcf_ota.img" "mcf_ota"
flash_firmware_both_slots "firmware/mcupm.img" "mcupm"
flash_firmware_both_slots "firmware/md1img.img" "md1img"
flash_firmware_both_slots "firmware/mvpu_algo.img" "mvpu_algo"
flash_firmware_both_slots "firmware/pi_img.img" "pi_img"
flash_firmware_both_slots "firmware/preloader_raw.img" "preloader_raw"
flash_firmware_both_slots "firmware/scp.img" "scp"
flash_firmware_both_slots "firmware/spmfw.img" "spmfw"
flash_firmware_both_slots "firmware/sspm.img" "sspm"
flash_firmware_both_slots "firmware/tee.img" "tee"
flash_firmware_both_slots "firmware/tkv.img" "tkv"
flash_firmware_both_slots "firmware/vcp.img" "vcp"
# Clear existing partitions
process_partitions_for_slots "clear" \
        "system" \
        "vendor" \
        "product" \
        "system_ext" \
        "vendor_dlkm" \
        "odm_dlkm" \
        "tr_carrier" \
        "tr_company" \
        "tr_mi" \
        "tr_overlayfs" \
        "tr_preload" \
        "tr_product" \
        "tr_region" \
        "tr_theme"

# Create new partitions
case "$SLOT" in
    "_a") OTHER_SLOT="_b" ;;
    "_b") OTHER_SLOT="_a" ;;
    *) ui_print "- Unknown boot slot: $SLOT"; exit 1 ;;
esac

create_partitions_for_slot "$SLOT" "$OTHER_SLOT" \
        "system:846479360" \
        "vendor:2147311616" \
        "product:3412955136" \
        "system_ext:2869694464" \
        "vendor_dlkm:21954560" \
        "odm_dlkm:348160" \
        "tr_carrier:348160" \
        "tr_company:348160" \
        "tr_mi:348160" \
        "tr_overlayfs:348160" \
        "tr_preload:307412992" \
        "tr_product:203460608" \
        "tr_region:348160" \
        "tr_theme:348160"

ui_print " "
ui_print "Patching system..."
flash_firmware_both_slots "boot.img" "boot"
flash_firmware_both_slots "dtbo.img" "dtbo"
flash_firmware_both_slots "vendor_boot.img" "vendor_boot"
flash_firmware_both_slots "vbmeta.img" "vbmeta"
flash_firmware_both_slots "vbmeta_system.img" "vbmeta_system"
flash_firmware_both_slots "vbmeta_vendor.img" "vbmeta_vendor"
flash_partition_zstd "system.img.zst" "/dev/block/mapper/system$SLOT"
flash_partition_zstd "vendor.img.zst" "/dev/block/mapper/vendor$SLOT"
flash_partition_zstd "product.img.zst" "/dev/block/mapper/product$SLOT"
flash_partition_zstd "system_ext.img.zst" "/dev/block/mapper/system_ext$SLOT"
flash_partition_zstd "vendor_dlkm.img.zst" "/dev/block/mapper/vendor_dlkm$SLOT"
flash_partition_zstd "odm_dlkm.img.zst" "/dev/block/mapper/odm_dlkm$SLOT"
flash_partition_zstd "tr_carrier.img.zst" "/dev/block/mapper/tr_carrier$SLOT"
flash_partition_zstd "tr_company.img.zst" "/dev/block/mapper/tr_company$SLOT"
flash_partition_zstd "tr_mi.img.zst" "/dev/block/mapper/tr_mi$SLOT"
flash_partition_zstd "tr_overlayfs.img.zst" "/dev/block/mapper/tr_overlayfs$SLOT"
flash_partition_zstd "tr_preload.img.zst" "/dev/block/mapper/tr_preload$SLOT"
flash_partition_zstd "tr_product.img.zst" "/dev/block/mapper/tr_product$SLOT"
flash_partition_zstd "tr_region.img.zst" "/dev/block/mapper/tr_region$SLOT"
flash_partition_zstd "tr_theme.img.zst" "/dev/block/mapper/tr_theme$SLOT"

# Final target-slot unmapping and mapping to ensure proper mounting
process_partitions_for_slot "unmap_map" "$SLOT" \
        "system" \
        "vendor" \
        "product" \
        "system_ext" \
        "vendor_dlkm" \
        "odm_dlkm" \
        "tr_carrier" \
        "tr_company" \
        "tr_mi" \
        "tr_overlayfs" \
        "tr_preload" \
        "tr_product" \
        "tr_region" \
        "tr_theme"

exit 0
