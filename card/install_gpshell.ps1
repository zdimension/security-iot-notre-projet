# Define the GitHub repository URL and API endpoint
$repoUrl = "https://api.github.com/repos/kaoh/globalplatform"
$latestReleaseUrl = "$repoUrl/releases/latest"

# Use the Invoke-RestMethod cmdlet to fetch the latest release information
$latestRelease = Invoke-RestMethod -Uri $latestReleaseUrl

# Get the name of the latest release and its assets
$latestReleaseName = $latestRelease.name
$assets = $latestRelease.assets

# make dir ./tmp
mkdir ./tmp

# Get the desktop path
$dl_path = "./tmp"

# Loop through the assets and download them to the desktop
foreach ($asset in $assets) {
    $assetName = $asset.name
    $assetUrl = $asset.browser_download_url

    # Define the download path on the desktop
    $downloadPath = Join-Path -Path $dl_path -ChildPath $assetName

    # Download the asset
    Invoke-WebRequest -Uri $assetUrl -OutFile $downloadPath

    Write-Host "Downloaded $assetName to $downloadPath"
}

Expand-Archive -Path "$dl_path\*.zip" -DestinationPath $dl_path

# copy $dlpath\gpshell*\bin\* to .\, ignore if already exists
Copy-Item -Path "$dl_path\gpshell*\bin\*" -Destination .\

# remove $dl_path
Remove-Item -Path $dl_path -Recurse

# run check_gpshell.ps1
powershell -ExecutionPolicy Bypass -File check_gpshell.ps1