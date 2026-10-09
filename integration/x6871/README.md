# Speed Lock — Infinix GT 20 Pro (`X6871`) Unified Integration Framework

**Location:** `integration/x6871/`  
**Classification:** Defensive Integration Architecture, Capability Engine & Diagnostic Framework  
**Target Hardware:** Infinix GT 20 Pro (`X6871`), MediaTek Dimensity 8200 Ultimate (`MT6896`)  
**Target Kernel:** Linux 5.10.237 GKI 2.0 (`5.10.237-android12-9-00014-gf82f7360927e-ab14119954`)  

---

## 1. Architectural Overview

The `integration/x6871/` framework provides an isolated, modular, and testable integration layer inside the permanent `Speed Lock` workspace. It bridges device firmware forensics with independently maintained exploit research backends without cross-polluting source trees or conflating metadata compatibility with binary ABI readiness.

```text
integration/x6871/
├── README.md                      # Architecture guide, lifecycle state definitions, test documentation
├── manifest/
│   ├── device_manifest.json       # Authoritative capability manifest for Infinix GT 20 Pro (X6871)
│   └── manifest_schema.json       # JSON Schema enforcing manifest structure
├── core/
│   ├── backend_interface.py       # IExploitBackend contract and BackendState lifecycle definitions
│   ├── device_identifier.py       # Firmware and device metadata extraction engine (build.prop parser)
│   ├── capability_engine.py       # Evaluates device profile against registered backends
│   └── diagnostic_logger.py       # Evidentiary logger (VERIFIED, STRONGLY_INDICATED, UNVERIFIED, DISPROVED)
├── backends/
│   ├── dfroot_backend.py          # DFRoot adapter (CVE-2026-43284, DirtyFrag)
│   ├── ghostlock_backend.py       # GhostLock adapter (CVE-2026-43499, IonStack)
│   └── backend_registry.py        # Central registry managing available backend adapters
├── tools/
│   ├── abi_validator.py           # Deep 64-bit ELF parser, vermagic checker, and symbol CRC validator
│   ├── build_validator.py         # Static toolchain, Kbuild tree, and repository dependency auditor
│   └── firmware_inspector.py      # High-level inspector for extracted stock firmware directories
└── tests/
    ├── test_device_identifier.py  # Unit tests for manifest, build.prop, and firmware inspection
    ├── test_capability_engine.py  # Unit tests for capability evaluation & blocker collection
    ├── test_abi_validator.py      # Unit tests for ELF parsing, vermagic, and modversions CRCs (with edge cases)
    ├── test_backend_registry.py   # Unit tests for backend plugin lookup and CVE queries
    ├── test_manifest_schema.py    # Schema validation tests and negative test cases
    ├── test_regression_profiles.py# Cross-device regression test suite across diverse kernel/hardware profiles
    ├── test_build_validator.py    # Unit tests for host toolchains and repository source audits
    └── run_all_tests.py           # Automated testbench runner
```

---

## 2. Explicit Capability Lifecycle States

To ensure transparency and prevent false claims of support, every backend evaluates to one of five explicit lifecycle states:

1. **`UNSUPPORTED`:** Hardware, SoC architecture, or kernel configuration is fundamentally incompatible (e.g. missing `CONFIG_XFRM` or non-ARM64 architecture).
2. **`UNTESTED`:** Device metadata or firmware artifacts are unconfirmed or incomplete.
3. **`METADATA_COMPATIBLE`:** High-level kernel configuration, candidate libraries, and partition maps match, but prebuilt binaries have not passed ELF ABI verification.
4. **`ABI_COMPATIBLE`:** Kernel modules pass deep binary validation (vermagic equals target kernel release and all required symbol CRCs match).
5. **`VERIFIED_CONFIRMED`:** Authorized end-to-end testing on physical hardware has succeeded in obtaining root access.

---

## 3. Running the Verification Testbench

Execute the automated test suite directly via Python 3:

```bash
python integration/x6871/tests/run_all_tests.py
```

All 30 unit, integration, and regression tests validate:
- Manifest deserialization and schema compliance against `manifest_schema.json`.
- Negative testing of corrupted manifest types, invalid architectures, and unlisted backend states.
- Firmware directory and `build.prop` parsing against extracted `Image-x6871-5.10.237`, `vendor_ramdisk_build.prop`, and `.config`.
- Strict ABI rejection of circulating `dfroot-android12-5.10.ko` (`5.10.252-dirty`) and non-ELF/32-bit corrupt files.
- Regression testing across Pixel 7, Galaxy S22+, Fire Max 11, and legacy 32-bit platforms.
- Toolchain and repository dependency auditing across all 5 workspace source trees.
