package org.tagUtil.type;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.tagUtil.constants.Format;
import org.tagUtil.constants.RegexMatches;
import org.tagUtil.service.AudioMethods;
import org.tagUtil.service.ImageMethods;
import org.tagUtil.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NewMusic {

    private static final Logger logger = LogManager.getLogger(NewMusic.class);

    public static void loopDirectory(File parentFolder) {
        File[] folders = parentFolder.listFiles();

        assert folders != null;
        //TODO cleanup empty hard folder
        createHardFolder(parentFolder);
        for (File folder : folders)
        {
            try {
                if (AudioMethods.isMultiDiscs(folder)) {
                    logger.info("*** SKIPPING ***: " + folder.getName());
                    moveToHardFolder(parentFolder,folder);
                    //TODO Handle: disc vs CD vs numbering
                } else if (!folder.getName().equals("HardFolder")) {
                    ImageMethods.extractArtFiles(folder);
                    processFolder(folder);
                }

            } catch (Exception e) {
                logger.error("Error on: " + folder.getAbsolutePath(), e);
            }
        }
    }


    private static void createHardFolder(File folder) {
        File directory = new File(folder.getAbsolutePath() + File.separator + "HardFolder");
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    private static void moveToHardFolder(File parentFolder, File folder) {
        var newDir = new File(parentFolder + File.separator + "HardFolder" + File.separator + folder.getName());
        try {
            FileUtils.moveDirectory(folder, newDir);
        } catch (Exception e) {
            logger.error("Exception! ", e);
        }
    }

    public static void processFolder(File folder) throws CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        File[] files = folder.listFiles();
        assert files != null;

        for (File file : files) {
            if (ImageMethods.isImage(file)) {
                ImageMethods.handleImage(file);
            } else if (Format.isJunk(file)){
                file.delete();
            } else if (Format.isAudio(file)) {
                AudioMethods.cleanUpAudioFile(file);
            }
        }

        var tag = FileHelper.getFirstTag(folder);
        var source = Paths.get(folder.getAbsolutePath());
        assert tag != null;

        // Rename the folder name to match: (YEAR) ALBUM_NAME
        var newFolderName = "(" + tag.getFirst(FieldKey.YEAR) + ")" + StringUtils.SPACE + tag.getFirst(FieldKey.ALBUM);

        // Remove illegal characters on Windows OS
        newFolderName = newFolderName.replaceAll(RegexMatches.ILLEGAL_REGEX_DASH, StringUtils.EMPTY).replaceAll(RegexMatches.ILLEGAL_REGEX_BLANK, StringUtils.EMPTY);
        Files.move(source, source.resolveSibling(newFolderName));
    }
}
