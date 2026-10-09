<#
.SYNOPSIS
    Check-Upstream.ps1 - Compares local repository heads against upstream GitHub remotes.
.DESCRIPTION
    Safely queries upstream remotes using git ls-remote to check synchronization status
    without modifying local working trees or discarding local changes. Logs all actions
    to logs/upstream_check_<timestamp>.log.
#>

[CmdletBinding()]
param(
    [string]$WorkspaceRoot = "C:\Users\Admin\Videos\Github\Speed Lock",
    [string]$RepositoriesDir = "repositories",
    [switch]$AsJson,
    [string]$OutputFile
)

$ErrorActionPreference = "Continue"

$LogDir = Join-Path $WorkspaceRoot "logs"
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}
$LogFile = Join-Path $LogDir "upstream_check_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

function Log-Message {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $line = "[$timestamp] [$Level] $Message"
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

Log-Message "Starting upstream status check for Speed Lock workspace: $WorkspaceRoot"

# Run discovery first
$discoverScript = Join-Path $WorkspaceRoot "scripts\Discover-Repositories.ps1"
$discovered = & $discoverScript -WorkspaceRoot $WorkspaceRoot -RepositoriesDir $RepositoriesDir

$results = @()

foreach ($repo in $discovered) {
    Log-Message "Checking upstream for [$($repo.Category)/$($repo.Name)] (Branch: $($repo.Branch))..."
    
    $syncStatus = "Unknown"
    $remoteSha = "N/A"
    $errorMsg = ""

    if ($repo.RemoteUrl -eq "N/A") {
        $syncStatus = "NoRemote"
        Log-Message "  Skipped: No remote configured." "WARN"
    } else {
        try {
            $remoteBranchRef = "refs/heads/$($repo.Branch)"
            $lsRemoteOutput = git ls-remote $repo.RemoteUrl $remoteBranchRef 2>&1
            $exitCode = $LASTEXITCODE

            if ($exitCode -eq 0 -and $lsRemoteOutput) {
                # Format: <sha>\t<ref>
                $firstLine = ($lsRemoteOutput | Select-Object -First 1).ToString().Trim()
                if ($firstLine -match '^([0-9a-fA-F]{40})\s+') {
                    $remoteSha = $matches[1]
                    if ($remoteSha -eq $repo.CommitSha) {
                        $syncStatus = "UpToDate"
                        Log-Message "  Status: Up-to-date (SHA: $($repo.ShortSha))" "SUCCESS"
                    } else {
                        $syncStatus = "DivergedOrBehind"
                        Log-Message "  Status: Local SHA ($($repo.ShortSha)) differs from Remote SHA ($($remoteSha.Substring(0,7)))" "WARN"
                    }
                } else {
                    $syncStatus = "RemoteRefNotFound"
                    Log-Message "  Ref $remoteBranchRef not found on remote." "WARN"
                }
            } else {
                $syncStatus = "QueryFailed"
                $errorMsg = ($lsRemoteOutput -join ' ')
                Log-Message "  git ls-remote failed: $errorMsg" "ERROR"
            }
        } catch {
            $syncStatus = "Exception"
            $errorMsg = $_.ToString()
            Log-Message "  Exception querying upstream: $errorMsg" "ERROR"
        }
    }

    $results += [PSCustomObject]@{
        Name            = $repo.Name
        Category        = $repo.Category
        LocalPath       = $repo.LocalPath
        RemoteUrl       = $repo.RemoteUrl
        Branch          = $repo.Branch
        LocalCommitSha  = $repo.CommitSha
        RemoteCommitSha = $remoteSha
        SyncStatus      = $syncStatus
        ErrorMessage    = $errorMsg
    }
}

Log-Message "Upstream check complete. Total repos evaluated: $($results.Count)"

if ($OutputFile) {
    if ($AsJson) {
        $results | ConvertTo-Json -Depth 5 | Set-Content -Path $OutputFile -Encoding UTF8
    } else {
        $results | Export-Csv -Path $OutputFile -NoTypeInformation -Encoding UTF8
    }
    Log-Message "Results saved to $OutputFile"
}

if ($AsJson -and -not $OutputFile) {
    $results | ConvertTo-Json -Depth 5
} else {
    return $results
}
