@echo off

set VERSION="1.0.0"
set GROUP_ID="com.uid2"
set ARTIFACT_ID="uid2-shared"

echo "uid2-shared: build & install"
mvn clean package && mvn install:install-file -Dfile="./target/%ARTIFACT_ID%-%VERSION%.jar" -Dsources="./target/%ARTIFACT_ID%-%VERSION%-sources.jar" -DgroupId="%GROUP_ID%" -DartifactId="%ARTIFACT_ID%" -Dpackaging=jar -DpomFile="./pom.xml" -Dversion="%VERSION%"
