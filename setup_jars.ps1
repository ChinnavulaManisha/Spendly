# Script to download required JAR dependencies for Tomcat & Java Compilation
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$destWebInf = "WebContent\WEB-INF\lib"
$destLib = "lib"

New-Item -ItemType Directory -Force -Path $destWebInf | Out-Null
New-Item -ItemType Directory -Force -Path $destLib | Out-Null

$downloads = @(
    @{ Url = "https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar"; File = "$destWebInf\javax.servlet-api-4.0.1.jar" },
    @{ Url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"; File = "$destWebInf\mysql-connector-j-8.3.0.jar" },
    @{ Url = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"; File = "$destWebInf\gson-2.10.1.jar" }
)

foreach ($item in $downloads) {
    if (-not (Test-Path $item.File)) {
        Write-Host "Downloading $($item.File)..."
        Invoke-WebRequest -Uri $item.Url -OutFile $item.File -UseBasicParsing
    }
}

Copy-Item "$destWebInf\*" -Destination $destLib -Force
Write-Host "Dependencies successfully placed in $destWebInf and $destLib."
Get-ChildItem $destWebInf
