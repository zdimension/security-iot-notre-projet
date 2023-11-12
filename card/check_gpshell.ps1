# List of required DLL file names
$requiredFiles = @(
    "globalplatform.dll",
    "gppcscconnectionplugin.dll",
    "gpshell.exe",
    "legacy.dll",
    "libcrypto-1_1.dll",
    "libcrypto-3.dll",
    "libssl-1_1.dll",
    "libssl-3.dll",
    "vcruntime140.dll",
    "zlibwapi.dll"
)

$missingFiles = $requiredFiles | ForEach-Object {
    $filePath = Join-Path -Path $PSScriptRoot -ChildPath $_
    if (-not (Test-Path $filePath)) {
        $_
    }
}

if ($missingFiles.Count -gt 0) {
    Write-Host "The following files are missing: $($missingFiles -join ', ')"
    exit 1  # Return error code 1
} else {
    Write-Host "All required files are present."
    exit 0  # Return success code
}
