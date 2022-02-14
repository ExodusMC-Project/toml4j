package com.moandjiezana.toml.testutils;

import java.io.File;
import java.util.Objects;

public class Utils {

    public static File file(Class<?> aClass, String file) {
        return new File(Objects.requireNonNull(aClass.getResource(file + ".toml")).getFile());
    }
}
