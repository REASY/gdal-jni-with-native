#!/usr/bin/env bash

set -eu

if [[ -z "$1" || -z "$2" ]]
  then
    echo "Missing mandatory arguments: PROJ_VERSION, GDAL_VERSION"
    exit 1
fi

PROJ_VERSION=$1
GDAL_VERSION=$2

docker build --build-arg PROJ_VERSION=$PROJ_VERSION --build-arg GDAL_VERSION=v"${GDAL_VERSION}"  \
  -f docker/gdal-ubuntu-small.dockerfile -t qartez-engine/gdal:"${GDAL_VERSION}" docker/

