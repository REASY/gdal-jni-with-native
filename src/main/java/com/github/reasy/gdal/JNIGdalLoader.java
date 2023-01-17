package com.github.reasy.gdal;

import com.sun.jna.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JNIGdalLoader {
    public static final String[] MODULES_TO_LOAD;

    public static final boolean IS_LOADED;

    public static final String PATH_TO_MODULES = String.format("native/%s.txt", Platform.RESOURCE_PREFIX);

    static {
        try {
            MODULES_TO_LOAD = GetModules();
            System.out.printf("JNIGdalLoader java: loading %d shared library...\n", MODULES_TO_LOAD.length);

            NativeLibraryLoader loader = new NativeLibraryLoader("/native");
            for (String module : MODULES_TO_LOAD) {
                loader.load(module);
            }
            System.out.println("JNIGdalLoader java: loaded all shared libraries");
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

    static String[] GetModules() throws IOException {
        List<String> modules = new ArrayList<>();
        try (InputStream inputStream = JNIGdalLoader.class.getClassLoader().getResourceAsStream(PATH_TO_MODULES)) {
            Objects.requireNonNull(inputStream, String.format("Resource as stream for '%s' is null", PATH_TO_MODULES));
            try (InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(streamReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    modules.add(line);
                }
            }
        }
        return modules.toArray(new String[0]);
    }

}
