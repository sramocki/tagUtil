package org.tagUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.tagUtil.util.AudioMethods;
import org.tagUtil.util.FileHelper;
import org.tagUtil.util.Format;
import org.tagUtil.util.RegexMatches;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    extractArtFiles(folder);
                    processFolder(folder);
                }

            } catch (Exception e) {
                logger.error("Error on: " + folder.getAbsolutePath(), e);
            }
        }
    }

    /**
     * If a subfolder named 'Art' exists, then move its contents up a folder and remove the 'Art' folder.
     * @param folder
     */
    private static void extractArtFiles(File folder) {
        var artFolder = new File(folder.getAbsolutePath() + File.separatorChar + "Art");
        if (artFolder.exists()) {
            logger.debug("FOUND ART! on " + folder.getAbsolutePath());
            for (File file : artFolder.listFiles()) {
                file.renameTo(new File(artFolder.getParent() + File.separator + file.getName()));
            }
            artFolder.delete();
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
            if (FileHelper.isImage(file)) {
                FileHelper.handleImage(file);
            } else if (FileHelper.isJunk(file)){
                file.delete();
            } else if (FileHelper.isAudio(file)) {
                cleanUpAudioFile(file);
            }
        }

        var tag = FileHelper.getFirstTag(folder);
        var source = Paths.get(folder.getAbsolutePath());
        assert tag != null;

        // Rename the folder name to match: (YEAR) ALBUM_NAME
        var newFolderName = "(" + tag.getFirst(FieldKey.YEAR) + ")" + StringUtils.SPACE + tag.getFirst(FieldKey.ALBUM);

        // Remove illegal characters on Windows OS
        newFolderName = newFolderName.replaceAll(FileHelper.ILLEGAL_REGEX, StringUtils.EMPTY);
        Files.move(source, source.resolveSibling(newFolderName));
    }

    public static void cleanUpAudioFile(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        AudioFile audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();

        tag.deleteArtworkField();
        tag.deleteField(FieldKey.COMMENT);

        String track = tag.getFirst(FieldKey.TRACK);
        track = track.replaceAll("/.*", "");
        tag.setField(FieldKey.TRACK, StringUtils.leftPad(track, 2, "0"));

        tag.deleteField(FieldKey.MUSICIP_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_DISC_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_TRACK_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_WORK_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_ORIGINAL_RELEASE_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASE_TRACK_ID);
        tag.deleteField(FieldKey.MUSICBRAINZ_ARTISTID);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASEARTISTID);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASEID);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASE_STATUS);
        tag.deleteField(FieldKey.MUSICBRAINZ_RELEASE_TYPE);
        tag.deleteField(FieldKey.ACOUSTID_ID);
        tag.deleteField(FieldKey.AMAZON_ID);
        tag.deleteField(FieldKey.ACOUSTID_FINGERPRINT);
        audioFile.commit();

        String outputTitle = tag.getFirst(FieldKey.TITLE).replaceAll(FileHelper.ILLEGAL_REGEX, "");
        if (outputTitle.length() > 150) {
            outputTitle = outputTitle.substring(0, 150);
        }

        // Create a Pattern object

        Pattern pattern1 = Pattern.compile(RegexMatches.MOVEMENT_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern1.matcher(outputTitle);
        if (matcher.find()) {
            String newTitle = matcher.replaceFirst("$1");
        }

        FileHelper.renameFile(file, tag.getFirst(FieldKey.TRACK) + " - " + outputTitle + "." + Format.FLAC);
    }
}
