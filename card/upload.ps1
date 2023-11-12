# Define the path to gpshell.exe and the script to upload
$gpshellPath = "gpshell.exe"
$uploadScript = "upload.gp"

# Run gpshell.exe with the upload script
Start-Process -FilePath $gpshellPath -ArgumentList $uploadScript -Wait -NoNewWindow
