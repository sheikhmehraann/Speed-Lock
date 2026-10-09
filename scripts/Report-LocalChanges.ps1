<#
.SYNOPSIS
    Report-LocalChanges.ps1 - Reports any uncommitted, untracked, or diverged changes across the archive.
.DESCRIPTION
    Safely inspects every repository working tree in the Speed Lock workspace before any synchronization
    or update operations. Never modifies, discards, or resets local changes.
#>

[CmdletBinding()]
param(
    [string]$WorkspaceRoot = "C:\Users\Admin\Videos\Github\Speed Lock",
    [string]$RepositoriesDir = "repositories",
    [switch]$Detailed,
    [string]$OutputFile
)

$ErrorActionPreference = "Continue"

$discoverScript = Join-Path $WorkspaceRoot "scripts\Discover-Repositories.ps1"
$discovered = & $discoverScript -WorkspaceRoot $WorkspaceRoot -RepositoriesDir $RepositoriesDir

$reports = @()

foreach ($repo in $discovered) {
    Push-Location $repo.LocalPath
    try {
        $statusLines = (git status --porcelain=v1 2>&1)
        $hasChanges = ($null -ne $statusLines -and $statusLines.Count -gt 0 -and (-not [string]::IsNullOrWhiteSpace($statusLines[0])))

        $modified = @()
        $untracked = @()
        $staged = @()

        if ($hasChanges) {
            foreach ($line in $statusLines) {
                if ($line.Length -ge 3) {
                    $indexState = $line.Substring(0, 1)
                    $workTreeState = $line.Substring(1, 1)
                    $filePath = $line.Substring(3).Trim()

                    if ($indexState -match '[MADRC]') {
                        $staged += "$indexState $filePath"
                    }
                    if ($workTreeState -match '[MD]') {
                        $modified += "$workTreeState $filePath"
                    }
                    if ($indexState -eq '?' -or $workTreeState -eq '?') {
                        $untracked += $filePath
                    }
                }
            }
        }

        # Check ahead / behind compared to tracking branch
        $trackingBranch = (git rev-parse --abbrev-ref --symbolic-full-name "@{u}" 2>$null)
        $aheadBehind = "NoTracking"
        if ($LASTEXITCODE -eq 0 -and $trackingBranch) {
            $counts = (git rev-list --left-right --count HEAD...@{u} 2>$null)
            if ($counts -match '(\d+)\s+(\d+)') {
                $ahead = [int]$matches[1]
                $behind = [int]$matches[2]
                $aheadBehind = "Ahead: $ahead, Behind: $behind"
            }
        }

        $reports += [PSCustomObject]@{
            Name            = $repo.Name
            Category        = $repo.Category
            HasLocalChanges = $hasChanges
            StagedCount     = $staged.Count
            ModifiedCount   = $modified.Count
            UntrackedCount  = $untracked.Count
            AheadBehind     = $aheadBehind
            StagedFiles     = ($staged -join "; ")
            ModifiedFiles   = ($modified -join "; ")
            UntrackedFiles  = ($untracked -join "; ")
            LocalPath       = $repo.LocalPath
        }
    } finally {
        Pop-Location
    }
}

$dirtyCount = ($reports | Where-Object { $_.HasLocalChanges }).Count
Write-Host "Scanned $($reports.Count) repositories. Repositories with local changes: $dirtyCount" -ForegroundColor $(if ($dirtyCount -gt 0) { "Yellow" } else { "Green" })

if ($OutputFile) {
    $reports | ConvertTo-Json -Depth 5 | Set-Content -Path $OutputFile -Encoding UTF8
    Write-Host "Report saved to $OutputFile"
}

return $reports
