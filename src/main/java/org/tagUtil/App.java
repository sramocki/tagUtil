package org.tagUtil;

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
import org.tagUtil.util.AudioMethods;
import org.tagUtil.util.FileHelper;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//TODO
/**
 * Multiple discs
 * Error reporting for missing tags
 * Handle A-Z structure for output
 * https://musicbrainz.org/doc/MusicBrainz_API
 */

public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {

        var startTime = System.currentTimeMillis();

        // Disables the default logging behavior of jaudiotagger
        java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
        try {
            manager.readConfiguration(App.class.getClassLoader().getResourceAsStream("audioLogger.config"));
        } catch (Exception e) {
            logger.error("Unable to configure audio logging, exiting...", e);
            return;
        }

        logger.info("Starting...");

        if (args[0] != null) {
            logger.info("You chose to open this directory: " + args[0]);
            loopDirectory(new File(args[0]));
        } else {
            var jFileChooser = new JFileChooser();
            jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            File folder;
            var status = jFileChooser.showSaveDialog(null);
            if (status == JFileChooser.APPROVE_OPTION) {
                folder = jFileChooser.getSelectedFile();
                logger.info("You chose to open this directory: " + folder.getAbsolutePath());
                loopDirectory(folder);
            }
        }

        var endTime = System.currentTimeMillis();
        logger.info("Finished in " + (endTime - startTime) + " ms");
    }

    private static void loopDirectory(File parentFolder) {
        File[] folders = parentFolder.listFiles();

        assert folders != null;
        //TODO cleanup empty hard folder
        createHardFolder(parentFolder);
        for (File folder : folders)
        {
            try {
                if (folder.getName().equals("HardFolder")) {
                    continue;
                } else if (isMultiDiscs(folder)) {
                    logger.info("*** SKIPPING ***: " + folder.getName());
                    moveToHardFolder(parentFolder,folder);
                    //TODO Handle: disc vs CD vs numbering
                } else {
                    preProcessFolder(folder);
                    processFolder(folder);
                }

            } catch (Exception e) {
                logger.error("Error on: " + folder.getAbsolutePath(), e);
            }
        }
    }

    private static void preProcessFolder(File folder) {
        var artFolder = new File(folder.getAbsolutePath() + File.separatorChar + "Art");
        if (artFolder.exists()) {
            logger.debug("FOUND ART! on " + folder.getAbsolutePath());
            for (File file : artFolder.listFiles()) {
                file.renameTo(new File(artFolder.getParent() + File.separator + file.getName()));
            }
            artFolder.delete();
        }
    }

    // TODO rewrite
    private static boolean isMultiDiscs(File folder) {
        List<File> fileList = Arrays.stream(folder.listFiles())
                .filter(File::isDirectory)
                .collect(Collectors.toList());
        return fileList.size() > 1;
    }

    private static void processFolder(File folder) throws CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        File[] files = folder.listFiles();
        assert files != null;

        for (File file : files) {
            if (FileHelper.isImage(file)) FileHelper.handleImage(file);
            if (FileHelper.isJunk(file)) file.delete();
            if (!FileHelper.isAudio(file)) continue;
            AudioMethods.cleanUpAudioFile(file);
        }

        var tag = FileHelper.getFirstTag(folder);
        var source = Paths.get(folder.getAbsolutePath());
        assert tag != null;
        var newFolderName = "(" + tag.getFirst(FieldKey.YEAR) + ")" + StringUtils.SPACE + tag.getFirst(FieldKey.ALBUM);

        // Remove illegal characters on Windows OS
        newFolderName = newFolderName.replaceAll(FileHelper.ILLEGAL_REGEX, "");
        Files.move(source, source.resolveSibling(newFolderName));
    }

    private static void createHardFolder(File folder) {
        File directory = new File(folder.getAbsolutePath() + File.separator + "HardFolder");
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    private static void moveToHardFolder(File parentFolder, File folder) {
        logger.info("I made it here!");
        var newDir = new File(parentFolder + File.separator + "HardFolder" + File.separator + folder.getName());
        logger.info(folder);
        logger.info(newDir);
        try {
            FileUtils.moveDirectory(folder, newDir);
        } catch (Exception e) {
            logger.error("Exception! ", e);
        }
    }
}