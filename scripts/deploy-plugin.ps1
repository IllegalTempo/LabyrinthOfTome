<#
Builds the Maven project and copies the produced jar to the specified server plugins folder.
Usage:
  .\deploy-plugin.ps1 -ServerPluginsPath "C:\path\to\server\plugins" -JarName "LabyrinthOfTome-1.0-SNAPSHOT.jar"
Parameters:
  -ServerPluginsPath (required): Destination plugins folder
  -JarName (optional): Name of the jar in target\ (default: uses project finalName heuristic)
  -SkipTests (optional): Pass -SkipTests to skip mvn tests (default: $false)
Example:
  .\deploy-plugin.ps1 -ServerPluginsPath "C:\spigot\plugins" -JarName "LabyrinthOfTome-1.0-SNAPSHOT.jar"
#>
param(
    [Parameter(Mandatory=$true)] [string] $ServerPluginsPath,
    [string] $JarName = "",
    [switch] $SkipTests
)

if (-not (Test-Path $ServerPluginsPath)) {
    Write-Error "Destination plugins folder not found: $ServerPluginsPath"
    exit 1
}

$skip = ''
if ($SkipTests) { $skip = ' -DskipTests' }
Write-Host "Building project with mvn package$skip..."
Invoke-Expression "mvn package$skip"

if ($LASTEXITCODE -ne 0) { Write-Error "Maven build failed"; exit 1 }

# Try to determine artifact name if not provided
$targetDir = Join-Path -Path (Get-Location) -ChildPath 'target'
if ($JarName -eq "") {
    $jarFiles = Get-ChildItem -Path $targetDir -Filter '*.jar' | Where-Object { $_.Name -notlike '*original*' } | Sort-Object LastWriteTime -Descending
    if ($jarFiles.Count -eq 0) { Write-Error "No jar found in target/"; exit 1 }
    $JarName = $jarFiles[0].Name
}

$src = Join-Path $targetDir $JarName
$dst = Join-Path $ServerPluginsPath $JarName

Write-Host "Copying $src -> $dst"
Copy-Item -Path $src -Destination $dst -Force
Write-Host "Copied. Check server logs to confirm plugin reloaded or restart the server if needed."

