Write-Host "Downloading Maven..."
Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile "maven.zip"
Write-Host "Unzipping Maven..."
Expand-Archive -Path "maven.zip" -DestinationPath "." -Force
$mavenBin = Join-Path (Get-Location) "apache-maven-3.9.9\bin"
$env:PATH = "$mavenBin;" + $env:PATH
Write-Host "Generating Maven Wrapper..."
mvn wrapper:wrapper
Write-Host "Cleaning up..."
Remove-Item -Path "maven.zip" -Force
Remove-Item -Recurse -Force -Path "apache-maven-3.9.9"
Write-Host "Done! You can now run .\mvnw clean javafx:run"
