package com.github.reasy.gdal;

import com.sun.jna.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public static final String TEMP_PATH_FOR_PROJ_DATA;

    static {
        if (System.getenv("PROJ_DATA") == null) {
            throw new IllegalStateException("There must be an env variable `PROJ_DATA` set to writable (preferably temporary) folder, for example `/tmp/gdal-jni-with-native/proj/`");
        }
        TEMP_PATH_FOR_PROJ_DATA = System.getenv("PROJ_DATA");

        try {
            MODULES_TO_LOAD = getModules();
            System.out.printf("JNIGdalLoader: loading %d shared library...\n", MODULES_TO_LOAD.length);

            NativeLibraryLoader loader = new NativeLibraryLoader("/native");
            for (String module : MODULES_TO_LOAD) {
                loader.load(module);
            }
            System.out.println("JNIGdalLoader: loaded all shared libraries");

            copyProjDb();

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

    private static void copyProjDb() throws IOException {
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream("/" + PROJ_DB_PATH)) {
            Objects.requireNonNull(in, String.format("Resource as stream for '%s' is null", PROJ_DB_PATH));
            Files.createDirectories(Paths.get(TEMP_PATH_FOR_PROJ_DATA));
            String destPath = TEMP_PATH_FOR_PROJ_DATA + "/proj.db";
            Files.copy(in, Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("JNIGdalLoader java: Copied resource at %s to %s\n", PROJ_DB_PATH, destPath);
        }
    }

}
