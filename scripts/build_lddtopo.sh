#!/usr/bin/env bash

set -eu

if [[ -z "$1" ]]
  then
    echo "Missing mandatory arguments: GDAL_VERSION"
    exit 1
fi

GDAL_VERSION=$1

docker build --build-arg IMAGE="qartez-engine/gdal:$GDAL_VERSION" -f docker/lddtopo.dockerfile \
  -t qartez-engine/lddtopo:"${GDAL_VERSION}" docker/

