# Speed Lock — Detection Engineering & Mitigation Reference

This guide outlines defensive techniques, purple-team detection rules, and platform mitigations against the kernel exploitation techniques catalogued in the Speed Lock archive.

---

## 1. Detection Engineering (DirtyFrag: CVE-2026-43284 / CVE-2026-43500)

The repository [`Dirty-Frag-hunting`](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/repositories/dirtyfrag/Dirty-Frag-hunting) provides operational detection artifacts:

### A. YARA Signatures (`dirtyfrag.yar`)
Five distinct detection rules target:
1. **Source Identifiers:** Detecting exploit compilation strings, unique xfrm socket structures, and known author fingerprints.
2. **Binary Artifacts:** Detecting static musl-compiled DirtyFrag ELF binaries across x86_64 and AArch64.
3. **Memory Signatures:** Scanning process address spaces for suspicious mapped page cache modifications.
4. **Tamper Indicators:** Monitoring `/system/lib64/libbase.so` or `/etc/passwd` for anomalous in-memory page divergence from disk storage.

### B. Sigma & Auditd Rules (`dirtyfrag.yml`)
Six Sigma rules for Linux auditd and enterprise SIEM platforms:
- **Rule 1:** Detecting anomalous creation of raw xfrm sockets by untrusted user accounts.
- **Rule 2:** Tracking `unshare(CLONE_NEWUSER)` chained with network namespace creation.
- **Rule 3:** Monitoring `aa-exec` nesting (detecting the Ubuntu AppArmor unprivileged user namespace bypass).
- **Rule 4:** Detecting unexpected writes into shared system libraries via page cache write patterns.

### C. Host Exposure Auditing (`dirtyfrag_hunt.py`)
Python inspection tool that checks:
- Kernel version exposure against mainline patched commits.
- Availability of unprivileged user namespaces (`kernel.unprivileged_userns_clone`).
- AppArmor sysctls (`kernel.apparmor_restrict_unprivileged_userns`).
- `/proc/net/xfrm_stat` for abnormal protocol errors indicative of exploit attempts.

---

## 2. Platform Mitigations & Vendor Defenses

### A. Samsung Knox & Real-Time Kernel Protection (RKP)
- **Mechanism:** Samsung RKP runs in EL2 (Hypervisor) and monitors critical kernel structures (`cred`, `task_struct`, `selinux_state`).
- **Impact on Exploits:** Directly modifying `task_struct->cred` triggers an immediate RKP exception and panic.
- **Bypass Observed:** Exploit authors bypass this by avoiding direct credential manipulation—instead redirecting usermode helper execution (`call_usermodehelper`) or utilizing `init` Unix stream sockets.

### B. Android App Seccomp & User Service Launchers
- **Mechanism:** Untrusted Android apps are strictly sandboxed by seccomp filters blocking system calls like `unshare` or raw socket creation.
- **Bypass Observed:** The `ghostlock-app` architecture utilizes **Shizuku** or ADB to run an unconstrained `UserService` shell process, completely stepping outside app seccomp boundaries before triggering the kernel exploit.

### C. SELinux Domain Transition Enforcements
- **Mechanism:** Strict SELinux policies prevent `init` (`u:r:init:s0`) from executing external binaries without transitioning to lower-privileged domains (`execute_no_trans` absent).
- **Impact on Exploits:** Demonstrated in `DirtyInit`: executing binaries under `init` causes an irrecoverable domain transition. The attack instead binds a stream socket directly inside the init namespace to execute commands natively.
