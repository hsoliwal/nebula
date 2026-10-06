# SPDX-License-Identifier: Apache-2.0
param([string]$Root, [string]$Source, [string]$Manifest, [string]$Context)
$ErrorActionPreference = 'Stop'
$seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
function Assert-NoReparse([string]$candidate) {
    $current = [System.IO.Path]::GetFullPath($candidate)
    while ($current) {
        if ($seen.Add($current)) {
            $entry = Get-Item -LiteralPath $current -Force -ErrorAction SilentlyContinue
            if ($entry -and ($entry.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
                throw "WINDOWS_REPARSE_CUSTODY_REFUSAL: $current"
            }
        }
        $parent = [System.IO.Path]::GetDirectoryName($current)
        if ($parent -eq $current) { break }
        $current = $parent
    }
}
Assert-NoReparse $Root
Assert-NoReparse $Source
foreach ($row in (Get-Content -LiteralPath $Manifest -Encoding UTF8)) {
    $relative = $row.Split([char]9)[0]
    Assert-NoReparse (Join-Path $Root $relative)
    Assert-NoReparse (Join-Path $Source $relative)
}
foreach ($row in (Get-Content -LiteralPath $Context -Encoding UTF8)) {
    Assert-NoReparse (Join-Path $Root $row.Split([char]9)[0])
}
Write-Output 'VERIFIED Windows reparse custody: all planned source, target and ancestor paths'
