<#
Starts a Minecraft server JAR in debug mode (JDWP) so IntelliJ can attach.
Usage:
  .\start-server-debug.ps1 -JarPath "C:\path\to\spigot.jar" -Port 5005 -Xmx 2G
Parameters:
  -JarPath (required): Path to the server jar (spigot.jar / paper.jar)
  -Port (optional): Debug port (default 5005)
  -Xmx (optional): JVM max heap (default 1G)
  -Xms (optional): JVM initial heap (default 512M)
  -ExtraArgs (optional): Extra JVM or server args as a single string
Example:
  .\start-server-debug.ps1 -JarPath "C:\spigot\spigot.jar" -Port 5005 -Xmx 2G
#>
param(
    [Parameter(Mandatory=$true)] [string] $JarPath,
    [int] $Port = 5005,
    [string] $Xmx = "1G",
    [string] $Xms = "512M",
    [string] $ExtraArgs = ""
)

if (-not (Test-Path $JarPath)) {
    Write-Error "Jar not found: $JarPath"
    exit 1
}

$jdwp = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$Port"
$mem = "-Xms$Xms -Xmx$Xmx"
$cmd = "java $jdwp $mem $ExtraArgs -jar `"$JarPath`" nogui"

Write-Host "Starting server in debug mode on port $Port..."
Write-Host $cmd

# Start the process in the current console so you can see server output
iex $cmd

