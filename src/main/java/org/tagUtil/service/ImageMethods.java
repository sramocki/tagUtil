package org.tagUtil.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tagUtil.constants.RegexMatches;
import org.tagUtil.util.FileHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ImageMethods {

    private static final Logger logger = LogManager.getLogger(ImageMethods.class);

    public static boolean isImage(File file) { return FilenameUtils.isExtension(file.getName().toLowerCase(), "jpeg", "jpg", "png"); }

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

    public static List<File> getLocalImageList(File file) {
        List<File> fileList = new ArrayList<>();
        File parent = file.getParentFile();
        if (parent.isDirectory()) {
            for (File childFile : Objects.requireNonNull(parent.listFiles())) {
                if (isImage(childFile)) {
                    fileList.add(childFile);
                }
            }
        }
        return fileList;
    }

    public static boolean handleImage(File file) {
        String inputFileName = file.getName().toLowerCase();

        String currentName;
        if (RegexMatches.FOLDER_PATTERN.matcher(inputFileName).find()) {
            currentName = RegexMatches.FOLDER;
        } else if (RegexMatches.BACK_PATTERN.matcher(inputFileName).find()) {
            currentName = RegexMatches.BACK;
        } else {
            List<File> imageFileList = getLocalImageList(file);
            if (imageFileList.size() == 1) {
                currentName = RegexMatches.FOLDER;
            } else if (imageFileList.size() == 2) {
                if (FileHelper.getNumberInFile(imageFileList.get(0)) < FileHelper.getNumberInFile(imageFileList.get(1))) {
                    return FileHelper.renameFile(imageFileList.get(0), RegexMatches.FOLDER) && FileHelper.renameFile(imageFileList.get(1), RegexMatches.BACK);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        if (FileHelper.getFileSize(file) > 5000 || FilenameUtils.isExtension(inputFileName, "png")) {
            return ImageMethods.convert(file, currentName);
        } else if (!currentName.equals(file.getName())) {
            return FileHelper.renameFile(file, currentName);
        }

        return false;
    }

    //TODO
    public static File getArtFolder(File file) {
        Iterator<File> iterator = FileUtils.iterateFiles(file, null, true);
        while (iterator.hasNext()) {
            File iteratedFile = iterator.next();
            if (iteratedFile.isDirectory() && Pattern.compile("(scan)|(art)").matcher(iteratedFile.getName().toLowerCase()).find()) {
                return iteratedFile;
            }
        }
        return null;
    }

    //TODO
    public static boolean relocateArt() {
        return false;
    }

    /**
     * If a subfolder named 'Art' exists, then move its contents up a folder and remove the 'Art' folder.
     * @param folder
     */
    public static void extractArtFiles(File folder) {
        var artFolder = new File(folder.getAbsolutePath() + File.separatorChar + "Art");
        if (artFolder.exists()) {
            logger.debug("FOUND ART! on " + folder.getAbsolutePath());
            for (File file : artFolder.listFiles()) {
                file.renameTo(new File(artFolder.getParent() + File.separator + file.getName()));
            }
            artFolder.delete();
        }
    }
}

