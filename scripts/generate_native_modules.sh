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

PROJ_VERSION="9.1.1"
GDAL_VERSION="3.5.3"

# Build GDAL
"${SCRIPT_DIR}/build_gdal.sh" $PROJ_VERSION $GDAL_VERSION

docker run -it --entrypoint /bin/bash -u $(id -u):$(id -g) \
 -v $(pwd)/libs:/java_libs \
 qartez-engine/gdal:"$GDAL_VERSION" -c "cp /usr/share/java/gdal-*.jar /java_libs/"

# Build lddtopo
"${SCRIPT_DIR}/build_lddtopo.sh" $GDAL_VERSION

# Analyze dependencies, build DAG, run toposort and copy them to /resource folder
docker run -u $(id -u):$(id -g) \
  -v $(pwd)/src/main/resources/:/resources qartez-engine/lddtopo:"$GDAL_VERSION" /usr/share/java/libgdalalljni.so /resources

# Build and test GDAL native library
./gradlew build && java -cp build/libs/gdal-jni-with-native-3.5.3.0.jar com.github.reasy.gdal.GdalExample

