#!/bin/bash

mkdir dependencies
cd dependencies

echo 'enclave-attestation-api: download'
git clone git@github.com:UnifiedID2/enclave-attestation-api-java.git

VERSION=${1:-"1.0.0"}
GROUP_ID="com.uid2"
ARTIFACT_ID="enclave-attestation-api"

echo 'enclave-attestation-api: build & install'
pushd enclave-attestation-api-java || exit
mvn package && mvn install:install-file -Dfile="./target/$ARTIFACT_ID-$VERSION.jar" -DgroupId="$GROUP_ID" -DartifactId="$ARTIFACT_ID" -Dpackaging=jar -Dversion="$VERSION"
popd
