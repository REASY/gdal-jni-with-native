Docker images
=====

# GDAL image with native libraries
[gdal-ubuntu-small.dockerfile](gdal-ubuntu-small.dockerfile) is based on the one provided by GDAL team, [Small: osgeo/gdal:ubuntu-small-latest](https://github.com/OSGeo/gdal/tree/release/3.5/docker#small-osgeogdalubuntu-small-latest) with two modifications:
- Added swig and Java to be able to generate JNI
- [Removed the load of native library in gdal](https://github.com/OSGeo/gdal/blob/release/3.5/swig/include/java/gdal_java.i#L18). Proper load of all required native all libraries is the reason why this library exists. 

# lddtopo
[lddtopo.dockerfile](lddtopo.dockerfile) is built on top of gdal-ubuntu-small.dockerfile. It uses https://github.com/REASY/lddtopo-rs to build the dependency graph of a provided library and run topological sort to get the order in which they should be loaded. 