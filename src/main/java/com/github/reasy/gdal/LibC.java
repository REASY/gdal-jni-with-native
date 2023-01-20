package com.github.reasy.gdal;

import com.sun.jna.Library;

public interface LibC extends Library {
    int setenv(String name, String value, int overwrite);
}