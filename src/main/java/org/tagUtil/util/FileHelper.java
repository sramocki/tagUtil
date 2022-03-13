package org.tagUtil.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.tagUtil.constants.Format;

import java.io.File;
import java.io.IOException;

public class FileHelper {

    public static long getFileSize(File file) {
        return file.length() / 1024;
    }

    public static boolean renameFile(File file, String outputName) {
        String basePath = FilenameUtils.getFullPath(file.getPath());
        File newFile = new File(basePath + outputName);
        return file.renameTo(newFile);
    }

    public static int getNumberInFile(File file) {
        String numString = file.getName().toLowerCase().replaceAll("[^0-9]", "");
        if (StringUtils.isNotEmpty(numString)) {
            return Integer.parseInt(numString);
        }
        return 0;
    }

    public static FlacTag getFirstTag(File folder) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        File[] files = folder.listFiles();
        assert files != null;

        for (File file : files)
        {
            if (Format.isAudio(file)) {
                AudioFile audioFile = AudioFileIO.read(file);
                return (FlacTag) audioFile.getTag();
            }
        }
        return null;
    }
}

