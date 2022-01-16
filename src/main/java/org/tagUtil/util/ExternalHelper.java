package org.tagUtil.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExternalHelper {

    public static boolean convert(File oldFile, String currentAsset) {
        boolean converted = false;
        String outputPath = oldFile.getAbsolutePath().replace(oldFile.getName(), StringUtils.EMPTY) + currentAsset;
        if (new File (outputPath).exists()) {
            return false;
        }
        List<String> cmdList = new ArrayList<>();
        cmdList.add("magick");
        cmdList.add(oldFile.getPath());
        cmdList.add("-quality");
        cmdList.add("90");
        cmdList.add(outputPath);

        try {
            Process process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));
            BufferedReader errorInput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while (errorInput.readLine() != null) {
                System.out.println(errorInput.readLine());
            }
            process.waitFor();

            if (new File (outputPath).exists()) {
                oldFile.delete();
                converted = true;
            }

        } catch (Exception e) {
            System.out.println("Exception while running process! " + e.getStackTrace());
        }
        return converted;
    }
}

