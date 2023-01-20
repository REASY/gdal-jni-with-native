package com.github.reasy.gdal;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class GdalExample {
    private GdalExample() {
    }

    private static void runExamples() throws IOException {
        System.out.println("GDAL version: " + gdal.VersionInfo());

        System.out.println("########## showGpkgLayers ##########");
        showGpkgLayers();
        System.out.println("########## showGpkgLayers ##########");
        System.out.println();
        System.out.println();


        System.out.println("########## writeCogGeoTiff #########");
        writeCogGeoTiff();
        System.out.println("########## writeCogGeoTiff #########");
    }

    private static void writeCogGeoTiff() throws IOException {
        int xSize = 512;
        int ySize = 512;
        Random rnd = new Random();
        float[] data = new float[xSize * ySize];
        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                int offset = y * ySize + x;
                data[offset] = rnd.nextFloat();
            }
        }

        Driver drv = gdal.GetDriverByName("MEM");
        int type = gdalconstConstants.GDT_Float32;
        Dataset memDs = drv.Create("", xSize, ySize, 1, type);
        System.out.printf("Created %d x %d dataset with 1 band and type %d\n", xSize, ySize, type);

        int err = memDs.SetProjection("EPSG:32611");
        if (err != gdalconstConstants.CE_None) {
            throw new IllegalStateException(String.format("Could not set projection, error: %d", err));
        }
        memDs.GetRasterBand(1).WriteRaster(0, 0, xSize, ySize, data);
        memDs.BuildOverviews("NEAREST", new int[]{2, 4, 8, 16, 32});

        Path tiffTempFile = Files.createTempFile("cog-", ".tiff");
        Driver gtiffDrv = gdal.GetDriverByName("GTiff");
        gtiffDrv.CreateCopy(tiffTempFile.toAbsolutePath().toString(), memDs, new String[]{"COPY_SRC_OVERVIEWS=YES",
                "TILED=YES",
                "COMPRESS=LZW"});
        gtiffDrv.delete();
        memDs.delete();

        System.out.println("Wrote COG GeoTIFF to " + tiffTempFile);
    }

    private static void showGpkgLayers() {
        DataSource open = ogr.Open("src/main/resources/example.gpkg");
        System.out.println("Name: " + open.GetName());
        System.out.println("Layers: " + open.GetLayerCount());

        for (int layerIndex = 1; layerIndex <= open.GetLayerCount(); layerIndex++) {
            Layer layer = open.GetLayer(layerIndex);
            if (layer == null) {
                System.out.printf("Layer[%d] is null!\n", layerIndex);
            } else {
                System.out.printf("Layer[%d]: %s\n", layerIndex, layer.GetName());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("OS name: " + System.getProperty("os.name"));
        System.out.println("OS arch: " + System.getProperty("os.arch"));
        System.out.println("OS version: " + System.getProperty("os.version"));
        System.out.println("Java version: " + System.getProperty("java.version"));

        // Force to load GDAL JNI and all dependencies
        JNIGdalLoader.load();
        gdal.AllRegister();

        runExamples();
    }
}