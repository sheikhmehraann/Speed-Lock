# Speed Lock — Infinix GT 20 Pro (`X6871`) Validation & Revalidation Test Report

**Report Location:** `research/device-compatibility/X6871_VALIDATION_RESULTS.md`  
**Classification:** Empirical Revalidation Audit, Automated Testbench Execution Log & Build Audit  
**Target Hardware:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896`)  
**Target Kernel:** Linux 5.10.237 GKI 2.0 (`5.10.237-android12-9-00014-gf82f7360927e-ab14119954`)  
**Testbench Script:** `integration/x6871/tests/run_all_tests.py`  
**Validation Date:** 2026-10-09  

---

## 1. Revalidation Audit of Prior Claims

In accordance with Phase 1 instructions, all claims from the initial DFRoot audit were re-evaluated against primary binary evidence and classified:

| Audit Item | Claim Evaluated | Epistemological Status | Primary Reproducible Evidence |
|---|---|---|---|
| **1. Kernel Release & Vermagic** | Kernel is `5.10.237-android12-9-00014-gf82f7360927e-ab14119954` with full vermagic `5.10.237-android12-9-... SMP preempt mod_unload modversions aarch64` | **VERIFIED** | Extracted from `Image-x6871-5.10.237` at exact offsets `0x21316c0` (release string) and `0x214bd28` (vermagic). |
| **2. Vulnerability Window** | Target firmware is unpatched against CVE-2026-43284 (DirtyFrag) and CVE-2026-43499 (IonStack) | **STRONGLY INDICATED** | `vbmeta.img` AVB 2.0 descriptor registers `security_patch: 2026-07-01`; kernel compiled `2025-09-17`. Precedes August 2026 fixes. Dynamic trigger unverified on physical hardware. |
| **3. LKM Incompatibility Blocker** | Prebuilt module `dfroot-android12-5.10.ko` fails on X6871 | **VERIFIED** | Prebuilt vermagic is `5.10.252-dirty` and `__versions` section is empty (`size=0`). Kernel strictly enforces `CONFIG_MODVERSIONS=y` and `check_modinfo()`, returning `-ENOEXEC`. |
| **4. Prior Checker Depth** | `Check-DFRoot-X6871-Compatibility.py` validated full ABI compatibility | **DISPROVED** | Old checker only performed string searches on `.config` and file presence checks; did not parse `__versions` or compare symbol CRCs. Replaced by `AbiValidator`. |
| **5. Target Library & Insmod Presence** | `/vendor/lib64/libbinderdebug.so` and `/vendor/bin/insmod` exist on device | **VERIFIED** | `vendor.map:3631` confirms `libbinderdebug.so` at block 281940–281952. `installed-files-vendor.txt:2913` confirms `insmod` as 13-byte symlink to `toybox_vendor`. |
| **6. GKI KMI Applicability** | Google GKI 2.0 KMI applies to Dimensity 8200 Ultimate | **VERIFIED** (Kernel KMI) / **UNVERIFIED** (Userspace SEPolicy) | All 226 vendor `.ko` modules import core ACK symbols. Userspace Transsion integrity services (`trancriticalparavfy`) unverified. |
| **7. OEM Transsion Properties** | Transsion OEM identity and platform code confirmed | **VERIFIED** | Parsed directly from `vendor_ramdisk_build.prop`: `ro.product.vendor.brand=Infinix`, `ro.product.vendor.model=Infinix X6871`, `ro.vendor.mediatek.platform=MT6895`, `ro.build.display.id=X6871-H962CF-U-BASE-260618V1066DevT`. |

---

## 2. Authoritative Symbol CRC Table for X6871 Kernel

Static extraction across all 226 vendor `.ko` modules inside `vendor_boot-ramdisk.cpio` revealed the exact 32-bit CRCs enforced by the running kernel for DFRoot's required imports:

| Kernel Symbol | Required CRC in X6871 Kernel | Prebuilt `dfroot-android12-5.10.ko` Status |
|---|---|---|
| `module_layout` | `0x7c24b32d` | **MISSING** (Section empty) |
| `register_kprobe` | `0xc502eb6d` | **MISSING** (Section empty) |
| `unregister_kprobe` | `0x18b23dc5` | **MISSING** (Section empty) |
| `printk` | `0xc5850110` | **MISSING** (Section empty) |
| `__stack_chk_fail` | `0x98a9d10c` | **MISSING** (Section empty) |
| `__stack_chk_guard` | `0x8f678b07` | **MISSING** (Section empty) |
| `memset` | `0xdcb764ad` | **MISSING** (Section empty) |

**Conclusion:** Merely rewriting the `vermagic` ASCII string in `.modinfo` is **insufficient**. Any module loaded into the X6871 kernel must contain an authentic `__versions` table matching the exact CRCs listed above, or `check_version()` will abort loading with `-ENOEXEC`.

---

## 3. Host Build Environment & Cross-Project Dependency Audit

Audited via `integration/x6871/tools/build_validator.py`:

### Cross-Project Repository Status
- **`DFRoot`**: `repositories/dirtyfrag/DFRoot` — **PRESENT** (all 5 required source files present: `exp.c`, `splicehelper.c`, `libcxx.S`, `dfroot.c`, `Makefile`)
- **`DirtyInit`**: `repositories/dirtyfrag/DirtyInit` — **PRESENT** (all required source files present: `dirtyinit.c`, `dfi_exploit.c`)
- **`GhostLock`**: `repositories/ghostlock-ionstack/GhostLock` — **PRESENT** (`rootchain.c` present)
- **`ghostlock-app`**: `repositories/ghostlock-ionstack/ghostlock-app` — **PRESENT** (`CMakeLists.txt`, `main.cpp`, `mtk-phys.sh` present)
- **`UniRoot`**: `repositories/root-management-apps/UniRoot` — **PRESENT** (`app/build.gradle.kts`, `build.gradle.kts`, `gradlew` present)

### Toolchain Status
- `javac.exe` / `java.exe`: Available (Microsoft OpenJDK 17)
- `rustc.exe` / `cargo.exe`: Available (Rust 1.90.0)
- `gcc.exe`: Available (MinGW 8.1.0)
- `clang` / `ld.lld` (cross aarch64): **NOT INSTALLED**
- `ndk-build` / Android NDK (`ANDROID_NDK_HOME`): **NOT INSTALLED**
- Kernel Build Tree (`KDIR`): **NOT CONFIGURED**

**Summary Verdict:** `SOURCE_AUDITED_TOOLCHAIN_MISSING`. All source repositories are fully intact and audited, but native compilation requires an aarch64 Clang toolchain and Linux Kbuild tree.

---

## 4. Automated Testbench Execution Results

Command executed:
```powershell
python integration\x6871\tests\run_all_tests.py
```

### Execution Log:
```text
======================================================================
 Speed Lock X6871 Integration Testbench Runner
======================================================================
test_32bit_elf_rejection (integration.x6871.tests.test_abi_validator.TestAbiValidator.test_32bit_elf_rejection) ... ok
test_corrupted_non_elf_binary (integration.x6871.tests.test_abi_validator.TestAbiValidator.test_corrupted_non_elf_binary) ... ok
test_mock_module_matching (integration.x6871.tests.test_abi_validator.TestAbiValidator.test_mock_module_matching) ... ok
test_nonexistent_file_handling (integration.x6871.tests.test_abi_validator.TestAbiValidator.test_nonexistent_file_handling) ... ok
test_prebuilt_mismatch_detection (integration.x6871.tests.test_abi_validator.TestAbiValidator.test_prebuilt_mismatch_detection) ... ok
test_default_backends_registered (integration.x6871.tests.test_backend_registry.TestBackendRegistry.test_default_backends_registered) ... ok
test_get_by_cve (integration.x6871.tests.test_backend_registry.TestBackendRegistry.test_get_by_cve) ... ok
test_get_by_id (integration.x6871.tests.test_backend_registry.TestBackendRegistry.test_get_by_id) ... ok
test_kbuild_readiness (integration.x6871.tests.test_build_validator.TestBuildValidator.test_kbuild_readiness) ... ok
test_repository_audit (integration.x6871.tests.test_build_validator.TestBuildValidator.test_repository_audit) ... ok
test_toolchain_detection (integration.x6871.tests.test_build_validator.TestBuildValidator.test_toolchain_detection) ... ok
test_validate_all_verdict (integration.x6871.tests.test_build_validator.TestBuildValidator.test_validate_all_verdict) ... ok
test_evaluate_dfroot_metadata_compatible (integration.x6871.tests.test_capability_engine.TestCapabilityEngine.test_evaluate_dfroot_metadata_compatible) ... ok
test_evaluate_ghostlock_metadata_compatible (integration.x6871.tests.test_capability_engine.TestCapabilityEngine.test_evaluate_ghostlock_metadata_compatible) ... ok
test_unsupported_on_missing_config (integration.x6871.tests.test_capability_engine.TestCapabilityEngine.test_unsupported_on_missing_config) ... ok
test_from_firmware_directory (integration.x6871.tests.test_device_identifier.TestDeviceIdentifier.test_from_firmware_directory) ... ok
test_from_manifest (integration.x6871.tests.test_device_identifier.TestDeviceIdentifier.test_from_manifest) ... ok
test_from_manifest_dynamic_flags (integration.x6871.tests.test_device_identifier.TestDeviceIdentifier.test_from_manifest_dynamic_flags) ... ok
test_parse_build_prop (integration.x6871.tests.test_device_identifier.TestDeviceIdentifier.test_parse_build_prop) ... ok
test_production_manifest_is_valid (integration.x6871.tests.test_manifest_schema.TestManifestSchema.test_production_manifest_is_valid) ... ok
test_rejects_invalid_architecture (integration.x6871.tests.test_manifest_schema.TestManifestSchema.test_rejects_invalid_architecture) ... ok
test_rejects_invalid_backend_state (integration.x6871.tests.test_manifest_schema.TestManifestSchema.test_rejects_invalid_backend_state) ... ok
test_rejects_missing_required_top_level (integration.x6871.tests.test_manifest_schema.TestManifestSchema.test_rejects_missing_required_top_level) ... ok
test_rejects_non_integer_page_size (integration.x6871.tests.test_manifest_schema.TestManifestSchema.test_rejects_non_integer_page_size) ... ok
test_schema_file_exists_and_parses (integration.x6871.tests.test_manifest_schema.TestManifestSchema.test_schema_file_exists_and_parses) ... ok
test_fire_max_11_disabled_modules (integration.x6871.tests.test_regression_profiles.TestRegressionProfiles.test_fire_max_11_disabled_modules) ... ok
test_legacy_32bit_arm_device (integration.x6871.tests.test_regression_profiles.TestRegressionProfiles.test_legacy_32bit_arm_device) ... ok
test_missing_futex_subsystem (integration.x6871.tests.test_regression_profiles.TestRegressionProfiles.test_missing_futex_subsystem) ... ok
test_pixel_7_missing_vendor_lib (integration.x6871.tests.test_regression_profiles.TestRegressionProfiles.test_pixel_7_missing_vendor_lib) ... ok
test_x6871_stock_profile (integration.x6871.tests.test_regression_profiles.TestRegressionProfiles.test_x6871_stock_profile) ... ok

----------------------------------------------------------------------
Ran 30 tests in 0.128s

OK
======================================================================
Tests run: 30
Failures: 0
Errors: 0
Skipped: 0
======================================================================
ALL TESTS PASSED SUCCESSFULLY.
```

---

## 5. Current Device Support Status Summary

| Backend Implementation | Status for X6871 | Prerequisites Met | Critical Remaining Blocker |
|---|---|---|---|
| **DFRoot (CVE-2026-43284)** | **METADATA_COMPATIBLE** | **YES** | Prebuilt LKM carries `5.10.252-dirty` and empty CRC table; requires compilation against ACK 5.10 commit `f82f7360927e` matching exact CRCs. |
| **GhostLock (CVE-2026-43499)** | **METADATA_COMPATIBLE** | **YES** | Missing compiled target header (`target.h` / `ionstack.conf`) with struct offsets for build `ab14119954` and MediaTek physical addresses (`kernel_phys_load` / `kernel_phys_offset`). |

**Overall Device Support Classification:** **INTEGRATION_IMPLEMENTED / METADATA_COMPATIBLE (NOT CONFIRMED OPERATIONAL)**.  
Live root access cannot be claimed until authorized physical device execution succeeds.
