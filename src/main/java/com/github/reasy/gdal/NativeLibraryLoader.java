package com.github.reasy.gdal;

import com.sun.jna.Platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a native library from the resource directory of the class.
 * Uses Platform.RESOURCE_PREFIX from <a href="https://github.com/java-native-access/jna">JNA</a> to load native libraries according to the running runtime
 *
 * Based on <a href="https://github.com/fmeum/rules_jni/blob/a428c306848d97587d953432d1ea29d72b819f21/jni/tools/native_loader/src/main/java/com/github/fmeum/rules_jni/RulesJni.java">RulesJNI</a>
 */
public class NativeLibraryLoader {

    static class NativeLibraryInfo {
        public final String path;
        public final File tempFile;

        NativeLibraryInfo(String path, File tempFile) {
            this.path = path;
            this.tempFile = tempFile;
        }
    }

    private static final Map<String, NativeLibraryInfo> LOADED_LIBS = new HashMap<>();

    private final String absolutePathToPackage;
    private final static Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("gdal_jni_with_native.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public NativeLibraryLoader(String absolutePathToPackage) {
        if (absolutePathToPackage == null) {
            throw new NullPointerException("absolutePathToPackage must not be null");
        }
        this.absolutePathToPackage = absolutePathToPackage;
    }

    public void load(final String name) {
        String path = absolutePathToPackage + "/" + libraryRelativePath(name);
        URL libraryResource = NativeLibraryLoader.class.getResource(path);
        failOnNullResource(libraryResource, name);
        load(name, libraryResource);
    }

    synchronized private static void load(String name, URL libraryResource) {
        String basename = libraryBasename(name);
        if (LOADED_LIBS.containsKey(basename)) {
            if (!libraryResource.toString().equals(LOADED_LIBS.get(basename).path)) {
                throw new UnsatisfiedLinkError(String.format(
                        "Cannot load two native libraries with same basename ('%s') from different paths\nFirst library: %s\nSecond library: %s\n",
                        basename, LOADED_LIBS.get(basename).path, libraryResource));
            }
            return;
        }
        Path tempFile;
        try {
            tempFile = extractLibrary(basename, libraryResource);
        } catch (IOException e) {
            throw new UnsatisfiedLinkError(e.getMessage());
        }
        System.load(tempFile.toAbsolutePath().toString());
        LOADED_LIBS.put(basename, new NativeLibraryInfo(libraryResource.toString(), tempFile.toFile()));
    }

    static String libraryRelativePath(final String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        String basename = libraryBasename(name);
        return String.format("%s/%s", Platform.RESOURCE_PREFIX, basename);
    }

    private static String libraryBasename(final String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    private static void failOnNullResource(final URL resource, final String name) {
        if (resource == null) {
            throw new UnsatisfiedLinkError(String.format(
                    "Failed to find native library '%s' in %s. Platform architecture is %s", name, Platform.RESOURCE_PREFIX, Platform.ARCH));
        }
    }

    private static Path extractLibrary(String basename, URL libraryResource) throws IOException {
        String mapped = System.mapLibraryName(basename);
        int lastDot = mapped.lastIndexOf('.');
        Path tempFile = Files.createTempFile(tempDir, mapped.substring(0, lastDot) + "_", mapped.substring(lastDot));
        try (InputStream in = libraryResource.openStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }
}
