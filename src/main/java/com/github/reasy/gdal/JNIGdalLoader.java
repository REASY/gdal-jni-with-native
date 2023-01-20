package com.github.reasy.gdal;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JNIGdalLoader {
    public static final String[] MODULES_TO_LOAD;
    public static final boolean IS_LOADED;
    public static final String PATH_TO_MODULES = String.format("native/%s.txt", Platform.RESOURCE_PREFIX);
    public static final String PROJ_DB_PATH = "proj/proj.db";
    public static final File TEMP_PATH_FOR_PROJ_DATA;

    public static final LibC libc;


    static {
        // TODO: Add support of Windows (need to load msvcrt and use `_putenv` instead
        // https://learn.microsoft.com/en-us/cpp/c-runtime-library/reference/putenv-wputenv?view=msvc-170
        libc = Native.load("c", LibC.class);

        try {
            TEMP_PATH_FOR_PROJ_DATA = Files.createTempDirectory("JNIGdalLoader").toFile();
            cleanUpOnShutdown();
            System.out.println("TEMP_PATH_FOR_PROJ_DATA: " + TEMP_PATH_FOR_PROJ_DATA);

            copyProjDb(TEMP_PATH_FOR_PROJ_DATA + "/proj.db");

            // Make PROJ used by GDAL aware of a path to proj.db via setting PROJ_DATA
            // https://proj.org/usage/environmentvars.html#envvar-PROJ_DATA
            final int err = libc.setenv("PROJ_DATA", TEMP_PATH_FOR_PROJ_DATA.getAbsolutePath(), 1);
            if (err != 0) {
                throw new IllegalStateException(String.format("Could not set env variable `PROJ_DATA` to %s", TEMP_PATH_FOR_PROJ_DATA));
            }

            // Get the list of modules to load
            MODULES_TO_LOAD = getModules();
            System.out.printf("JNIGdalLoader: loading %d shared library...\n", MODULES_TO_LOAD.length);

            // Load modules
            NativeLibraryLoader loader = new NativeLibraryLoader("/native");
            for (String module : MODULES_TO_LOAD) {
                loader.load(module);
            }
            System.out.println("JNIGdalLoader: loaded all shared libraries");


            IS_LOADED = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("STACKTRACE:");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static void load() {
        // Just make sure it that it is loaded.
        // If we reach here, it must always be true (because otherwise the RuntimeException should have been thrown before in static ctor)
        assert IS_LOADED;
    }

    private static String[] getModules() throws IOException {
        List<String> modules = new ArrayList<>();
        try (InputStream in = JNIGdalLoader.class.getClassLoader().getResourceAsStream(PATH_TO_MODULES)) {
            Objects.requireNonNull(in, String.format("Resource as stream for '%s' is null", PATH_TO_MODULES));
            try (InputStreamReader streamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(streamReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    modules.add(line);
                }
            }
        }
        return modules.toArray(new String[0]);
    }

    private static void copyProjDb(String destPath) throws IOException {
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream("/" + PROJ_DB_PATH)) {
            Objects.requireNonNull(in, String.format("Resource as stream for '%s' is null", PROJ_DB_PATH));
            Files.copy(in, Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("JNIGdalLoader java: Copied resource at %s to %s\n", PROJ_DB_PATH, destPath);
        }
    }

    private static void cleanUpOnShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                cleanUp();
            } catch (IOException ex) {
                System.err.printf("Could not clean-up TEMP_PATH_FOR_PROJ_DATA '%s'. Error: %s [%s]%n", TEMP_PATH_FOR_PROJ_DATA, ex.getClass(), ex.getMessage());
                ex.printStackTrace();
            }
        }));
    }

    private static void cleanUp() throws IOException {
        Files.delete(Paths.get(TEMP_PATH_FOR_PROJ_DATA + "/proj.db"));
        TEMP_PATH_FOR_PROJ_DATA.delete();
    }

}
