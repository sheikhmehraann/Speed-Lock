<#
.SYNOPSIS
    Check-Kernel510Support.ps1 - Audits Speed Lock archive repositories for Linux 5.10 & Android 5.10 compatibility.
.DESCRIPTION
    Scans cloned repositories for 5.10 kernel branches, precompiled LKMs (.ko), target header definitions,
    HOCON kernel profiles, and mentions of MediaTek MT6895/MT6896 / Dimensity 8200 / Transsion / Infinix X6871.
    Outputs a structured compatibility table and logs findings to logs/check_kernel510_<timestamp>.log.
#>

[CmdletBinding()]
param(
    [string]$WorkspaceRoot = "C:\Users\Admin\Videos\Github\Speed Lock"
)

$ErrorActionPreference = "Continue"

$LogDir = Join-Path $WorkspaceRoot "logs"
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}
$LogFile = Join-Path $LogDir "check_kernel510_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

function Log-Message {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] [$Level] $Message"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log-Message "Starting Kernel 5.10 & X6871 Target Compatibility Audit..."

$RepoBase = Join-Path $WorkspaceRoot "repositories"
$categories = Get-ChildItem -Path $RepoBase -Directory

$Results = @()

foreach ($cat in $categories) {
    $repos = Get-ChildItem -Path $cat.FullName -Directory
    foreach ($repo in $repos) {
        $repoPath = $repo.FullName
        $repoName = $repo.Name
        
        # Check active git branch and available branches
        $currentBranch = (git -C $repoPath rev-parse --abbrev-ref HEAD 2>$null)
        $branches = (git -C $repoPath branch -a 2>$null)
        $has510Branch = ($branches | Where-Object { $_ -match "5\.10" }) -ne $null

        # Search for 5.10 precompiled modules (.ko)
        $ko510 = Get-ChildItem -Path $repoPath -Recurse -Filter "*5.10*.ko" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name
        
        # Search for 5.10 profiles / targets / config files
        $conf510 = Get-ChildItem -Path $repoPath -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
            $_.Name -match "5\.10" -and ($_.Extension -in @(".conf", ".h", ".c", ".rs", ".json", ".md"))
        } | Select-Object -ExpandProperty Name

        # Search for MT6896 / MT6895 / Dimensity 8200 / Infinix / X6871 references
        $mtkMatches = Get-ChildItem -Path $repoPath -Recurse -File -ErrorAction SilentlyContinue | Where-Object {
            $_.Extension -in @(".c", ".h", ".cpp", ".hpp", ".md", ".json", ".kt", ".java", ".py")
        } | Select-String -Pattern "MT6896|MT6895|Dimensity 8200|X6871|X6873|X6876" -SimpleMatch -List

        # Classify 5.10 support level
        $supportLevel = "Unsupported / Not Explicit"
        $evidence = @()

        if ($has510Branch) {
            $supportLevel = "Source-Verified (Dedicated 5.10 Branch)"
            $evidence += "Dedicated git branch: $currentBranch"
        }
        if ($ko510) {
            $supportLevel = "Binary-Verified (Precompiled 5.10 LKM)"
            $evidence += "LKMs: $($ko510 -join ', ')"
        }
        if ($conf510) {
            if ($supportLevel -notmatch "Binary-Verified") {
                $supportLevel = "Target-Verified (5.10 Profile/Offset Present)"
            }
            $evidence += "5.10 Artifacts: $($conf510[0..2] -join ', ')"
        }

        # Check README / docs for 5.10 mentions
        $readme = Join-Path $repoPath "README.md"
        if (Test-Path $readme) {
            $readmeMatches = Select-String -Path $readme -Pattern "5\.10" -Context 0,0
            if ($readmeMatches -and $supportLevel -eq "Unsupported / Not Explicit") {
                $supportLevel = "Documented Lead (Mentions 5.10 in Docs)"
                $evidence += "README mentions 5.10 ($($readmeMatches.Count) occurrences)"
            }
        }

        $mtkRelevance = "No MT6896 / X6871 reference"
        if ($mtkMatches) {
            $matchedTerms = ($mtkMatches | ForEach-Object { $_.Line.Trim() }) -join "; "
            if ($matchedTerms.Length -gt 80) { $matchedTerms = $matchedTerms.Substring(0, 77) + "..." }
            $mtkRelevance = "Relevant: $matchedTerms"
        }

        $resultObj = [PSCustomObject]@{
            Repository    = $repoName
            Category      = $cat.Name
            SupportLevel  = $supportLevel
            Has510Branch  = [bool]$has510Branch
            LKMCount510   = ($ko510 | Measure-Object).Count
            Evidence      = ($evidence -join " | ")
            MT6896Status  = $mtkRelevance
        }

        $Results += $resultObj
    }
}

Log-Message "Audit finished. Found $($Results.Count) audited repositories."

# Print summary table
Write-Host "`n=== KERNEL 5.10 REPOSITORY AUDIT SUMMARY ===" -ForegroundColor Cyan
$Results | Where-Object { $_.SupportLevel -ne "Unsupported / Not Explicit" -or $_.MT6896Status -ne "No MT6896 / X6871 reference" } | Format-Table -Property Repository, Category, SupportLevel, LKMCount510, Evidence -Wrap -AutoSize

return $Results
