#!/bin/bash
echo "Downloading Maven..."
curl -s -L "https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -o maven.zip
echo "Unzipping Maven..."
unzip -q maven.zip
export PATH="$PWD/apache-maven-3.9.9/bin:$PATH"
echo "Generating Maven Wrapper..."
mvn wrapper:wrapper
echo "Cleaning up..."
rm maven.zip
rm -rf apache-maven-3.9.9
echo "Done! You can now run ./mvnw clean javafx:run"
