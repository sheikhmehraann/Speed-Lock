# Speed Lock — Workspace Rules & Operational Safety Standard

## 1. Permanent Workspace Identification

**Path:** `C:\Users\Admin\Videos\Github\Speed Lock`  
This directory is the permanent, dedicated workspace for the Speed Lock Android Kernel Security Research Archive. All clones, research documentation, technical inventories, automation scripts, logs, and audit reports must remain strictly confined to this workspace.

---

## 2. Scope & Research Objective

The Speed Lock archive is established exclusively for:
1. **Source Code & History Preservation:** Downloading publicly available research repositories, maintaining complete Git histories, tracking upstream branches, tags, and commit SHAs.
2. **Metadata & Vulnerability Indexing:** Cataloguing vulnerability identifiers (CVEs), targeted hardware platforms, system-on-chips (SoCs), Android OS releases, kernel baselines, bootloader requirements, and root persistence models.
3. **Comparative Architectural Research:** Analyzing differences between upstream vulnerability discoveries, reference implementations, device-specific ports, and detection signatures.
4. **Defensive Knowledge Engineering:** Documenting mitigation strategies, purple-team detection rules (Sigma, YARA), and kernel hardening mechanisms.

---

## 3. Strict Operational Safety Guardrails (Zero-Execution Policy)

In accordance with Step 6 of the workspace mandate, strict non-execution boundaries are enforced:

- **No Code Execution:** Downloaded ELF binaries, APK packages, shell scripts (`.sh`), Python exploit scripts, or compiled native artifacts must **never** be executed on the host system or any connected environment.
- **No Exploit Compilation or Deployment:** Do not build, make, cross-compile, or deploy exploit payloads or kernel modules.
- **No Device Interfacing:** Do not connect to physical or emulated devices via ADB, Fastboot, serial UART, or proprietary flashing protocols for testing exploits.
- **No Partition or Bootloader Alterations:** Do not flash partitions, modify bootloader states, alter Knox/SELinux security settings, or install unauthorized certificates.
- **Untrusted Input Posture:** All downloaded source code, submodules, releases, and documentation must be treated strictly as untrusted input. Static review, syntactic analysis, and documentation cross-referencing are the only authorized operations.

---

## 4. Preservation & Non-Destructive Principles

- **Preserve User & Existing Work:** Never delete, overwrite, clean (`git clean -fdx`), or reset (`git reset --hard`) existing user files, uncommitted changes, or existing repositories.
- **Case-Insensitive Filesystem Disambiguation:** When repository names collide under Windows NTFS case-insensitivity (e.g., `mitschud/DirtyFrag` vs `V4bel/dirtyfrag`), repositories must be disambiguated with owner prefixes rather than overwriting or collapsing directories.
- **Submodule & Remote Integrity:** Submodule references (`.gitmodules`) and Git LFS tracking (`.gitattributes`) must be preserved and kept intact.
- **Safe Synchronization:** Upstream checks must use non-mutating queries (`git ls-remote`) rather than disruptive pulls or checkouts.

---

## 5. Scripting & Automation Standards

All maintenance utilities located in `scripts/` must adhere to these standards:
- **Path Quoting:** All PowerShell commands and file operations must correctly handle Windows paths containing spaces.
- **Comprehensive Logging:** Script operations, errors, and skipped items must be logged to timestamped files under `logs/`.
- **Pre-Sync Dirty Tree Verification:** `Report-LocalChanges.ps1` must always inspect working trees before any sync operations to ensure zero loss of local work.
- **Non-Throwing Failures:** Scripts must implement defensive error handling (`try/catch/finally`) so failure on an individual repository does not abort batch processing.
