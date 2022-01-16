package org.tagUtil.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class FileHelper {

    public static final String FOLDER = "folder.jpg";
    public static final String BACK = "back.jpg";
    public static final String ILLEGAL_REGEX = "[\\\\/:*?\"<>|]";

    private static final Pattern folderPattern = Pattern.compile("(frontcover)|(folder)|(cover)|(front)");
    private static final Pattern backPattern = Pattern.compile("(backcover)|(back)|(tray)|(bck)|(inlay)|(rear)");

    public static long getFileSize(File file) {
        return file.length() / 1024;
    }

    public static boolean renameFile(File file, String outputName) {
        String basePath = FilenameUtils.getFullPath(file.getPath());
        File newFile = new File(basePath + outputName);
        return file.renameTo(newFile);
    }

    public static boolean isImage(File file) { return FilenameUtils.isExtension(file.getName().toLowerCase(), "jpeg", "jpg", "png"); }

    public static boolean isAudio(File file) { return FilenameUtils.isExtension(file.getName().toLowerCase(), Format.FLAC); }

    public static boolean isJunk(File file) { return FilenameUtils.isExtension(file.getName().toLowerCase(), "log", "cue", "m3u", "m3u8"); }

    private static List<File> getLocalImageList(File file) {
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

    private static int getNumberInFile(File file) {
        String numString = file.getName().toLowerCase().replaceAll("[^0-9]", "");
        if (StringUtils.isNotEmpty(numString)) {
            return Integer.parseInt(numString);
        }
        return 0;
    }

    public static boolean handleImage(File file) {
        String inputFileName = file.getName().toLowerCase();

        String currentName;
        if (folderPattern.matcher(inputFileName).find()) {
            currentName = FOLDER;
        } else if (backPattern.matcher(inputFileName).find()) {
            currentName = BACK;
        } else {
           List<File> imageFileList = getLocalImageList(file);
            if (imageFileList.size() == 1) {
                currentName = FOLDER;
            } else if (imageFileList.size() == 2) {
                if (getNumberInFile(imageFileList.get(0)) < getNumberInFile(imageFileList.get(1))) {
                    return renameFile(imageFileList.get(0), FOLDER) && renameFile(imageFileList.get(1), BACK);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        if (FileHelper.getFileSize(file) > 5000 || FilenameUtils.isExtension(inputFileName, "png")) {
            return ExternalHelper.convert(file, currentName);
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

    public static FlacTag getFirstTag(File folder) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        File[] files = folder.listFiles();
        assert files != null;

        for (File file : files)
        {
            if (FileHelper.isAudio(file)) {
                AudioFile audioFile = AudioFileIO.read(file);
                return (FlacTag) audioFile.getTag();
            }
        }
        return null;
    }
}

