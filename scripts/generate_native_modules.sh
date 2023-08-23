#!/usr/bin/env bash
set -eu

SCRIPT_DIR=$(dirname "$0")
case $SCRIPT_DIR in
    "/"*)
        ;;
    ".")
        SCRIPT_DIR=$(pwd)
        ;;
    *)
        SCRIPT_DIR=$(pwd)/$(dirname "$0")
        ;;
esac

export SCRIPT_DIR

PROJ_VERSION="9.2.1"
GDAL_VERSION="3.6.4"

# Build GDAL
"${SCRIPT_DIR}/build_gdal.sh" $PROJ_VERSION $GDAL_VERSION

docker run -it --entrypoint /bin/bash -v $(pwd)/libs:/java_libs \
 qartez-engine/gdal:"$GDAL_VERSION" -c "cp /usr/share/java/gdal-*.jar /java_libs/"

# Build lddtopo
"${SCRIPT_DIR}/build_lddtopo.sh" $GDAL_VERSION

# Analyze dependencies, build DAG, run toposort and copy them to /resource folder
docker run -v $(pwd)/src/main/resources/:/resources qartez-engine/lddtopo:"$GDAL_VERSION" /usr/share/java/libgdalalljni.so /resources

# Build and test GDAL native library
lib_name=$(./gradlew properties | grep ^name | sed 's/name: //g')
lib_version=$(./gradlew properties | grep ^version | sed 's/version: //g')
lib_full_name="$lib_name-$lib_version.jar"
./gradlew clean build && java -cp "build/libs/$lib_full_name" com.github.reasy.gdal.GdalExample

