<#
.SYNOPSIS
    Discover-Repositories.ps1 - Discovers all Git repositories in the archive workspace.
.DESCRIPTION
    Scans the Speed Lock workspace, discovers all Git repositories, extracts
    detailed local status (branch, SHA, commit date, dirty status, remotes, submodules, LFS),
    handles Windows paths with spaces, and returns structured objects.
#>

[CmdletBinding()]
param(
    [string]$WorkspaceRoot = "C:\Users\Admin\Videos\Github\Speed Lock",
    [string]$RepositoriesDir = "repositories",
    [switch]$AsJson,
    [string]$OutputFile
)

$ErrorActionPreference = "Continue"

$targetBase = Join-Path $WorkspaceRoot $RepositoriesDir
if (-not (Test-Path $targetBase)) {
    Write-Error "Repositories directory does not exist: $targetBase"
    return
}

# Traverse 2 levels down: repositories/<category>/<repo>
$discovered = @()
$categories = Get-ChildItem -Path $targetBase -Directory

foreach ($catDir in $categories) {
    $category = $catDir.Name
    $repoDirs = Get-ChildItem -Path $catDir.FullName -Directory

    foreach ($rDir in $repoDirs) {
        $gitDir = Join-Path $rDir.FullName ".git"
        if (-not (Test-Path $gitDir)) {
            continue
        }

        $repoDir = $rDir.FullName
        $repoName = $rDir.Name
        $relPath = "$RepositoriesDir\$category\$repoName"

        Push-Location $repoDir
        try {
            # Remote URL
            $remoteUrl = (git config --get remote.origin.url 2>$null)
            if (-not $remoteUrl) { $remoteUrl = "N/A" } else { $remoteUrl = $remoteUrl.Trim() }

            # Current branch
            $branch = (git rev-parse --abbrev-ref HEAD 2>$null)
            if (-not $branch) { $branch = "HEAD (detached)" } else { $branch = $branch.Trim() }

            # Commit SHA
            $commitSha = (git rev-parse HEAD 2>$null)
            if (-not $commitSha) { $commitSha = "N/A" } else { $commitSha = $commitSha.Trim() }

            $shortSha = (git rev-parse --short HEAD 2>$null)
            if (-not $shortSha) { $shortSha = "N/A" } else { $shortSha = $shortSha.Trim() }

            # Commit Date (ISO 8601)
            $commitDate = (git log -1 --format=%cI 2>$null)
            if (-not $commitDate) { $commitDate = "N/A" } else { $commitDate = $commitDate.Trim() }

            # Commit Author
            $commitAuthor = (git log -1 --format="%an <%ae>" 2>$null)
            if (-not $commitAuthor) { $commitAuthor = "N/A" } else { $commitAuthor = $commitAuthor.Trim() }

            # Commit Subject
            $commitSubject = (git log -1 --format=%s 2>$null)
            if (-not $commitSubject) { $commitSubject = "N/A" } else { $commitSubject = $commitSubject.Trim() }

            # Dirty status (working tree modifications or untracked)
            $statusOutput = (git status --porcelain 2>$null)
            $isDirty = ($null -ne $statusOutput -and $statusOutput.Count -gt 0 -and (-not [string]::IsNullOrWhiteSpace($statusOutput[0])))

            # Submodules
            $hasSubmodules = Test-Path ".gitmodules"
            $submoduleCount = 0
            if ($hasSubmodules) {
                $subLines = Select-String -Path ".gitmodules" -Pattern "\[submodule"
                $submoduleCount = $subLines.Count
            }

            # Git LFS
            $hasLfs = $false
            if (Test-Path ".gitattributes") {
                $lfsMatch = Select-String -Path ".gitattributes" -Pattern "filter=lfs" -SimpleMatch
                if ($lfsMatch) { $hasLfs = $true }
            }

            $discovered += [PSCustomObject]@{
                Name            = $repoName
                Category        = $category
                LocalPath       = $repoDir
                RelativePath    = $relPath
                RemoteUrl       = $remoteUrl
                Branch          = $branch
                CommitSha       = $commitSha
                ShortSha        = $shortSha
                CommitDate      = $commitDate
                Author          = $commitAuthor
                Subject         = $commitSubject
                IsDirty         = $isDirty
                HasSubmodules   = $hasSubmodules
                SubmoduleCount  = $submoduleCount
                HasLFS          = $hasLfs
            }
        } finally {
            Pop-Location
        }
    }
}

if ($OutputFile) {
    if ($AsJson) {
        $discovered | ConvertTo-Json -Depth 5 | Set-Content -Path $OutputFile -Encoding UTF8
    } else {
        $discovered | Export-Csv -Path $OutputFile -NoTypeInformation -Encoding UTF8
    }
}

if ($AsJson -and -not $OutputFile) {
    $discovered | ConvertTo-Json -Depth 5
} else {
    return $discovered
}
