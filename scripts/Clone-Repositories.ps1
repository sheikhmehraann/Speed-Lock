<#
.SYNOPSIS
    Clone-Repositories.ps1 - Clones all archive repositories safely into categorized folders.
.DESCRIPTION
    Clones the curated list of Android kernel security research repositories,
    preserves Git history, initializes submodules, pulls LFS content where applicable,
    and logs all progress to logs/clone.log.
#>

[CmdletBinding()]
param(
    [string]$WorkspaceRoot = "C:\Users\Admin\Videos\Github\Speed Lock",
    [switch]$SkipExisting = $true
)

$ErrorActionPreference = "Continue"

$LogDir = Join-Path $WorkspaceRoot "logs"
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}
$LogFile = Join-Path $LogDir "clone_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

function Log-Message {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] [$Level] $Message"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log-Message "Starting repository collection for Speed Lock workspace: $WorkspaceRoot"

# Curated list of 32 repositories
$Repositories = @(
    # GhostLock / IonStack Core
    @{ Name = "ghostlock-app"; Owner = "YuKongA"; Url = "https://github.com/YuKongA/ghostlock-app"; Category = "ghostlock-ionstack"; Folder = "ghostlock-app" },
    @{ Name = "ghostlock-a17"; Owner = "mobilehackinglab"; Url = "https://github.com/mobilehackinglab/ghostlock-a17"; Category = "ghostlock-ionstack"; Folder = "ghostlock-a17" },
    @{ Name = "GhostLock"; Owner = "R0rt1z2"; Url = "https://github.com/R0rt1z2/GhostLock"; Category = "ghostlock-ionstack"; Folder = "GhostLock" },

    # Device-Specific Projects (GhostLock / Root-My ports)
    @{ Name = "ghostlock-oneplus"; Owner = "JoinChang"; Url = "https://github.com/JoinChang/ghostlock-oneplus"; Category = "device-specific-projects"; Folder = "ghostlock-oneplus" },
    @{ Name = "GhostLock-Galaxy"; Owner = "wxxsfxyzm"; Url = "https://github.com/wxxsfxyzm/GhostLock-Galaxy"; Category = "device-specific-projects"; Folder = "GhostLock-Galaxy" },
    @{ Name = "GhostSam"; Owner = "snothin"; Url = "https://github.com/snothin/GhostSam"; Category = "device-specific-projects"; Folder = "GhostSam" },
    @{ Name = "ghostlock-emerald"; Owner = "datfooldive"; Url = "https://github.com/datfooldive/ghostlock-emerald"; Category = "device-specific-projects"; Folder = "ghostlock-emerald" },
    @{ Name = "oppo-ghostlock"; Owner = "pubglite55"; Url = "https://github.com/pubglite55/oppo-ghostlock"; Category = "device-specific-projects"; Folder = "oppo-ghostlock" },
    @{ Name = "meizu21-ghostlock-root"; Owner = "ymh001"; Url = "https://github.com/ymh001/meizu21-ghostlock-root"; Category = "device-specific-projects"; Folder = "meizu21-ghostlock-root" },
    @{ Name = "iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock"; Owner = "ankitrawatgit"; Url = "https://github.com/ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock"; Category = "device-specific-projects"; Folder = "iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock" },
    @{ Name = "Root-My-Galaxy"; Owner = "BuSung-dev"; Url = "https://github.com/BuSung-dev/Root-My-Galaxy"; Category = "device-specific-projects"; Folder = "Root-My-Galaxy" },
    @{ Name = "Root-My-Galaxy-Payloads"; Owner = "BuSung-dev"; Url = "https://github.com/BuSung-dev/Root-My-Galaxy-Payloads"; Category = "device-specific-projects"; Folder = "Root-My-Galaxy-Payloads" },
    @{ Name = "Root-My-Pixel"; Owner = "alex193a"; Url = "https://github.com/alex193a/Root-My-Pixel"; Category = "device-specific-projects"; Folder = "Root-My-Pixel" },
    @{ Name = "Root-My-Pixel-Payloads"; Owner = "alex193a"; Url = "https://github.com/alex193a/Root-My-Pixel-Payloads"; Category = "device-specific-projects"; Folder = "Root-My-Pixel-Payloads" },
    @{ Name = "Root-My-Device"; Owner = "tqmane"; Url = "https://github.com/tqmane/Root-My-Device"; Category = "device-specific-projects"; Folder = "Root-My-Device" },
    @{ Name = "root-my-nothing"; Owner = "ang3lo-azevedo"; Url = "https://github.com/ang3lo-azevedo/root-my-nothing"; Category = "device-specific-projects"; Folder = "root-my-nothing" },
    @{ Name = "pixel-ksu-root"; Owner = "JingMatrix"; Url = "https://github.com/JingMatrix/pixel-ksu-root"; Category = "device-specific-projects"; Folder = "pixel-ksu-root" },
    @{ Name = "Root-My-Device-Payloads"; Owner = "WitAqua-tools"; Url = "https://github.com/WitAqua-tools/Root-My-Device-Payloads"; Category = "device-specific-projects"; Folder = "Root-My-Device-Payloads" },

    # DirtyFrag and related research
    @{ Name = "DFRoot"; Owner = "diabl0w"; Url = "https://github.com/diabl0w/DFRoot"; Category = "dirtyfrag"; Folder = "DFRoot" },
    # Case disambiguation on Windows NTFS: mitschud-DirtyFrag vs V4bel-dirtyfrag
    @{ Name = "DirtyFrag"; Owner = "mitschud"; Url = "https://github.com/mitschud/DirtyFrag"; Category = "dirtyfrag"; Folder = "DirtyFrag-mitschud" },
    @{ Name = "DFReroot"; Owner = "polygraphene"; Url = "https://github.com/polygraphene/DFReroot"; Category = "dirtyfrag"; Folder = "DFReroot" },
    @{ Name = "DirtyInit"; Owner = "combeng6th"; Url = "https://github.com/combeng6th/DirtyInit"; Category = "dirtyfrag"; Folder = "DirtyInit" },
    @{ Name = "dirtyfrag"; Owner = "V4bel"; Url = "https://github.com/V4bel/dirtyfrag"; Category = "dirtyfrag"; Folder = "dirtyfrag-V4bel" },
    @{ Name = "dirtyfrag-rs"; Owner = "t0asts"; Url = "https://github.com/t0asts/dirtyfrag-rs"; Category = "dirtyfrag"; Folder = "dirtyfrag-rs" },
    @{ Name = "dirtyfrag-arm64"; Owner = "linnemanlabs"; Url = "https://github.com/linnemanlabs/dirtyfrag-arm64"; Category = "dirtyfrag"; Folder = "dirtyfrag-arm64" },
    @{ Name = "DirtyFrag-Galaxy"; Owner = "coey0814"; Url = "https://github.com/coey0814/DirtyFrag-Galaxy"; Category = "dirtyfrag"; Folder = "DirtyFrag-Galaxy" },
    @{ Name = "Dirty-Frag-hunting"; Owner = "0xAllow"; Url = "https://github.com/0xAllow/Dirty-Frag-hunting"; Category = "dirtyfrag"; Folder = "Dirty-Frag-hunting" },

    # Kernel Research & Exploits
    @{ Name = "CVE-2026-43499-popsicle"; Owner = "x-spy"; Url = "https://github.com/x-spy/CVE-2026-43499-popsicle"; Category = "kernel-research"; Folder = "CVE-2026-43499-popsicle" },
    @{ Name = "CyberMeowfia"; Owner = "NebuSec"; Url = "https://github.com/NebuSec/CyberMeowfia"; Category = "kernel-research"; Folder = "CyberMeowfia" },

    # Root Management Apps
    @{ Name = "lspromise"; Owner = "lsposed"; Url = "https://github.com/lsposed/lspromise"; Category = "root-management-apps"; Folder = "lspromise" },

    # Reference Catalogues
    @{ Name = "awesome-android-root-exploits"; Owner = "DuncanParSky"; Url = "https://github.com/DuncanParSky/awesome-android-root-exploits"; Category = "reference-catalogues"; Folder = "awesome-android-root-exploits" },
    @{ Name = "awesome-android-root"; Owner = "awesome-android-root"; Url = "https://github.com/awesome-android-root/awesome-android-root"; Category = "reference-catalogues"; Folder = "awesome-android-root" }
)

$summary = @{
    Total = $Repositories.Count
    Cloned = 0
    Reused = 0
    Failed = 0
    Skipped = 0
}

$CloneResults = @()

foreach ($repo in $Repositories) {
    $targetDir = Join-Path $WorkspaceRoot "repositories\$($repo.Category)\$($repo.Folder)"
    $gitDir = Join-Path $targetDir ".git"

    Log-Message "Processing [$($repo.Owner)/$($repo.Name)] -> Category: $($repo.Category)"

    $status = "Unknown"
    $commitSha = "N/A"
    $branch = "N/A"
    $commitDate = "N/A"
    $lfsStatus = "None"
    $submoduleStatus = "None"
    $errorMessage = ""

    if (Test-Path $gitDir) {
        if ($SkipExisting) {
            Log-Message "  Repository already exists at $targetDir. Inspecting local state..." "INFO"
            $status = "Reused"
            $summary.Reused++
        }
    } else {
        # Clone fresh
        Log-Message "  Cloning from $($repo.Url) to $targetDir..." "INFO"
        $parentDir = Split-Path $targetDir -Parent
        if (-not (Test-Path $parentDir)) {
            New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
        }

        $cloneOutput = git clone "$($repo.Url)" "$targetDir" 2>&1
        $exitCode = $LASTEXITCODE

        if ($exitCode -eq 0) {
            Log-Message "  Clone succeeded." "SUCCESS"
            $status = "Cloned"
            $summary.Cloned++
        } else {
            Log-Message "  Clone failed with exit code $exitCode. Output: ($($cloneOutput -join ' '))" "ERROR"
            $status = "Failed"
            $summary.Failed++
            $errorMessage = ($cloneOutput -join ' ')
        }
    }

    if (Test-Path $gitDir) {
        # Extract git metadata safely
        Push-Location $targetDir
        try {
            $branch = (git rev-parse --abbrev-ref HEAD 2>&1).Trim()
            $commitSha = (git rev-parse HEAD 2>&1).Trim()
            $commitDate = (git log -1 --format=%cI 2>&1).Trim()
            
            # Check for submodules
            if (Test-Path ".gitmodules") {
                Log-Message "  Initializing submodules..." "INFO"
                $subOutput = git submodule update --init --recursive 2>&1
                if ($LASTEXITCODE -eq 0) {
                    $submoduleStatus = "Initialized"
                } else {
                    $submoduleStatus = "Error: $($subOutput -join ' ')"
                }
            } else {
                $submoduleStatus = "None"
            }

            # Check for Git LFS
            if (Test-Path ".gitattributes") {
                $lfsAttr = Select-String -Path ".gitattributes" -Pattern "filter=lfs" -SimpleMatch
                if ($lfsAttr) {
                    Log-Message "  Git LFS detected, pulling LFS objects..." "INFO"
                    $lfsOutput = git lfs pull 2>&1
                    if ($LASTEXITCODE -eq 0) {
                        $lfsStatus = "Pulled"
                    } else {
                        $lfsStatus = "Error: $($lfsOutput -join ' ')"
                    }
                }
            }
        } catch {
            Log-Message "  Error inspecting metadata: $_" "WARN"
        } finally {
            Pop-Location
        }
    }

    $CloneResults += [PSCustomObject]@{
        Name = $repo.Name
        Owner = $repo.Owner
        Url = $repo.Url
        Category = $repo.Category
        Folder = $repo.Folder
        LocalPath = $targetDir
        Status = $status
        Branch = $branch
        CommitSha = $commitSha
        CommitDate = $commitDate
        Submodules = $submoduleStatus
        LFS = $lfsStatus
        ErrorMessage = $errorMessage
    }
}

Log-Message "Repository collection completed! Total: $($summary.Total), Cloned: $($summary.Cloned), Reused: $($summary.Reused), Failed: $($summary.Failed)"

$CloneResultsJsonPath = Join-Path $WorkspaceRoot "inventory\raw_clone_results.json"
$CloneResults | ConvertTo-Json -Depth 5 | Set-Content -Path $CloneResultsJsonPath -Encoding UTF8
Log-Message "Raw clone results saved to $CloneResultsJsonPath"

return $CloneResults
