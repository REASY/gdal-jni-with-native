gdal-jni-with-native
======
[<img src="https://jitpack.io/v/REASY/gdal-jni-with-native.svg">](https://jitpack.io/#REASY/gdal-jni-with-native)

This project builds GDAL, extracts native libraries and makes them available as Java library. At the moment it only supports GDAL on Ubuntu x64  (Linux)

### Build
In order to build the project the following should happen:
1. Build GDAL docker image, [build_gdal.sh](scripts.build_gdal.sh) is responsible for that
2. Build [lddtopo-rs](https://github.com/REASY/lddtopo-rs), [build_lddtopo.sh](scripts/build_lddtopo.sh) is responsible for that
3. Analyze the dependencies of /usr/share/java/libgdalalljni.so by building DAG and running topological sort on it
4. Copy all required native modules to `src/main/resources/native/%os%-%arch%`
5. Copy proj DB to [src/main/resources/proj/proj.db](src/main/resources/proj/proj.db)
7. Generate `src/main/resources/native/%os%-%arch%.txt` that contains new line separated list of modules to be loaded. The order comes from topological sort!

All of this is done in a script [generate_native_modules.sh](scripts/generate_native_modules.sh), just run it from root folder of the repo to get the final JAR
```bash
./scripts/generate_native_modules.sh
```
