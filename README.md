# Speed Lock — Android Kernel Security Research Archive

Welcome to the **Speed Lock** Android Kernel Security Research Archive. This workspace is a curated, organized repository archive and research environment focused on publicly available Android kernel security research, privilege escalation primitives, and device-specific rooting implementations.

---

## 1. Workspace Identity & Permanent Location

- **Permanent Workspace Path:** `C:\Users\Admin\Videos\Github\Speed Lock`
- **Scope:** Static source code archival, Git history preservation, vulnerability metadata tracking, architectural documentation, and automated inventory maintenance.
- **Strict Safety Constraint:** Zero execution of downloaded exploits, APKs, payloads, or shell scripts. Static analysis and documentation only.

---

## 2. Directory Architecture

The workspace is organized into standardized directories:

```text
Speed Lock/
├── README.md                           # Master project guide and usage handbook
├── WORKSPACE_RULES.md                  # Strict operational boundaries and safety rules
├── REPOSITORY_INDEX.md                 # Complete index of all 32 archived repositories
├── RESEARCH_STATUS.md                  # Comprehensive vulnerability analysis & state report
├── repositories/                       # Cloned public research repositories
│   ├── ghostlock-ionstack/             # Core IonStack & CVE-2026-43499 repositories
│   ├── dirtyfrag/                      # CVE-2026-43284 / 43500 implementations and detection tools
│   ├── device-specific-projects/       # OEM hardware ports (Samsung, Pixel, OnePlus, etc.)
│   ├── root-management-apps/           # Modern logic-to-root privilege escalation chains
│   ├── kernel-research/                # Upstream vulnerability research and target generators
│   └── reference-catalogues/           # Encyclopedic Android rooting catalogues & databases
├── inventory/                          # Structured metadata and audit outputs
│   ├── repositories.json               # Machine-readable inventory with full technical schema
│   ├── repositories.csv                # Tabular spreadsheet inventory for fast filtering
│   ├── clone-status.md                 # Detailed clone verification and sync table
│   └── duplicates.md                   # Duplicate, fork, and submodule lineage analysis
├── research/                           # Synthesized research documentation
│   ├── architecture/                   # Exploit architecture and primitive writeups
│   ├── device-compatibility/           # Hardware, SoC, and OS compatibility matrix
│   ├── upstream-history/               # Discovery timelines and kernel commit tracking
│   └── documentation/                  # Mitigation analysis and purple-team notes
├── scripts/                            # PowerShell archive automation utilities
│   ├── Discover-Repositories.ps1       # Discovers all Git repositories & extracts local state
│   ├── Check-Upstream.ps1              # Checks remote synchronization without mutating state
│   ├── Report-LocalChanges.ps1         # Inspects working trees for uncommitted changes
│   ├── Generate-Inventory.ps1          # Refreshes all inventory files and indexes
│   └── Clone-Repositories.ps1          # Clones or verifies all 32 repositories
├── logs/                               # Timestamped execution logs for all script operations
└── reports/                            # Archive verification and security audit reports
```

---

## 3. Archive Summary & Monitored Repositories

The archive manages **32 distinct repositories** across 6 technical categories:

| Category | Count | Key Focus | Representative Repositories |
|---|---|---|---|
| **GhostLock / IonStack** | 3 | Core IonStack / CVE-2026-43499 | `ghostlock-app`, `ghostlock-a17`, `GhostLock` |
| **DirtyFrag** | 9 | Deterministic page cache write (CVE-2026-43284) | `DFRoot`, `DirtyFrag-mitschud`, `dirtyfrag-V4bel`, `DirtyInit`, `Dirty-Frag-hunting` |
| **Device-Specific Projects** | 15 | OEM-specific locked-bootloader jailbreaks | `Root-My-Pixel`, `Root-My-Galaxy`, `ghostlock-oneplus`, `root-my-nothing`, `meizu21-ghostlock-root` |
| **Root Management Apps** | 1 | Logic-to-kernel escalation chains | `lspromise` (Android 17 Telecom logic 0-day + Kernel 1-day) |
| **Kernel Research** | 2 | Upstream research foundations & target builders | `CyberMeowfia`, `CVE-2026-43499-popsicle` |
| **Reference Catalogues** | 2 | Curated directories and exploit history | `awesome-android-root`, `awesome-android-root-exploits` |

For a complete tabular list with direct links and commit SHAs, consult [REPOSITORY_INDEX.md](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/REPOSITORY_INDEX.md).

---

## 4. Maintenance Scripts Handbook

All maintenance utilities are located in `scripts/` and designed for Windows PowerShell with full support for paths containing spaces:

### 1. Discover Repositories
Scans the archive, discovers all local Git repositories, and outputs their current checked-out branch, commit SHA, last update timestamp, submodules, and dirty status:
```powershell
.\scripts\Discover-Repositories.ps1
```
Or export directly as JSON:
```powershell
.\scripts\Discover-Repositories.ps1 -AsJson -OutputFile inventory\discovered.json
```

### 2. Inspect Local Modifications
Inspects all 32 working trees before any synchronization to ensure local changes are never overwritten:
```powershell
.\scripts\Report-LocalChanges.ps1
```

### 3. Check Upstream Status
Non-destructively checks local commit SHAs against upstream GitHub remotes using `git ls-remote`:
```powershell
.\scripts\Check-Upstream.ps1
```

### 4. Refresh Inventories
Re-analyzes all repositories, refreshes JSON, CSV, clone status, and indexes:
```powershell
.\scripts\Generate-Inventory.ps1
```

### 5. Clone or Re-verify Repositories
Idempotently verifies that all 32 repositories are present and their submodules initialized:
```powershell
.\scripts\Clone-Repositories.ps1
```

---

## 5. Future Session Continuity Workflow

When returning to this archive in future sessions:
1. **Always work in `C:\Users\Admin\Videos\Github\Speed Lock`**. Do not create a separate workspace.
2. **Inspect workspace state:** Run `.\scripts\Report-LocalChanges.ps1` to confirm working tree integrity.
3. **Check for upstream updates:** Run `.\scripts\Check-Upstream.ps1` to view changes on GitHub.
4. **Refresh inventories:** Run `.\scripts\Generate-Inventory.ps1` whenever new commits or research notes are added.
5. **Strictly adhere to [WORKSPACE_RULES.md](file:///C:/Users/Admin/Videos/Github/Speed%20Lock/WORKSPACE_RULES.md)**: Zero execution of untrusted binaries, APKs, or exploit scripts.
