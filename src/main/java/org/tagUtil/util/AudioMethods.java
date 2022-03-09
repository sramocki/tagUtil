package org.tagUtil.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AudioMethods {

    // TODO rewrite
    public static boolean isMultiDiscs(File folder) {
        List<File> fileList = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isDirectory)
                .collect(Collectors.toList());
        return fileList.size() > 1;
    }
}
