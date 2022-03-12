package org.tagUtil;

import org.apache.commons.collections4.Trie;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.tagUtil.Window.getComposerTrie;

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

    public static File cleanUpAudioFile(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        var commit = false;
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();

        logger.debug("Processing: " + file.getName());

        var track = tag.getFirst(FieldKey.TRACK);
        track = track.replaceAll("/.*", "");
        tag.setField(FieldKey.TRACK, StringUtils.leftPad(track, 2, "0"));
        tag.setField(FieldKey.DISC_NO, "1");
        audioFile.commit();
        tag.deleteArtworkField();
        tag.deleteField(FieldKey.COMMENT);
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

        var trackTitle = tag.getFirst(FieldKey.TITLE);

        if (trackTitle.contains(":")) {
            var composer = tag.getFirst(FieldKey.COMPOSER);
            var trackSplitByColons = trackTitle.split(":");
            var cleanupStart = false;

            // Cleanup missing composers in titles
            if (StringUtils.isEmpty(composer)) {
                var missingComposer = "`missing`";
                var partialComposer = trackSplitByColons[0];
                logger.debug("Found a partial composer: " + partialComposer);
                missingComposer = findComposerFromPartial(partialComposer.toLowerCase());
                if (!"`missing`".equals(missingComposer)) {
                    tag.setField(FieldKey.COMPOSER, missingComposer);
                    cleanupStart = true;
                }
            }

            trackTitle = tag.getFirst(FieldKey.TITLE);
            var firstColonIndex = trackTitle.indexOf(":");
            var firstSpaceIndex = trackTitle.indexOf(StringUtils.SPACE);

            // Cleanup colon in the start of a track title
            if (cleanupStart || firstColonIndex < firstSpaceIndex) {
                tag.setField(FieldKey.TITLE, trackTitle.substring(firstColonIndex + 2));
                audioFile.commit();
                commit = true;
            }

            trackTitle = tag.getFirst(FieldKey.TITLE);
            var lastColonIndex = trackTitle.lastIndexOf(":");
            firstSpaceIndex = trackTitle.indexOf(StringUtils.SPACE);

            // Cleanup colon in the end of a track title
            if (lastColonIndex > firstSpaceIndex) {
                var trackArray = trackTitle.substring(0, lastColonIndex).split(" ");
                var movementString = trackArray[trackArray.length-1];
                List<String> movementList = Arrays.asList("Aria", "Chorus", "Duetto", "Terzetto");
                if (!movementList.contains(movementString)) {
                    trackTitle = trackTitle.substring(0, lastColonIndex) + " -" + trackTitle.substring(lastColonIndex + 1);
                    tag.setField(FieldKey.TITLE, trackTitle);
                    audioFile.commit();
                    commit = true;
                }
            }
        }

        trackTitle = tag.getFirst(FieldKey.TITLE);

        // Cleanup track title length
        String outputTitle = trackTitle.replaceAll(FileHelper.ILLEGAL_REGEX, "-");
        if (outputTitle.length() > 100) {
            logger.warn("Output title exceeded 100 characters!");
            commit = true;
            outputTitle = outputTitle.substring(0, 100);
            logger.debug("Updated title to: " + outputTitle);
        }

        track = tag.getFirst(FieldKey.TRACK);
        String finalPath = track + " - " + outputTitle + "." + Format.FLAC;
        String finalFullPath = file.getParent() + File.separator + finalPath;

        // Rename track filename if needed
        if (commit || !file.getAbsolutePath().equals(finalFullPath)) {
            logger.debug("Updated filename to: " + finalPath);
            FileHelper.renameFile(file, finalPath);

            file = new File(finalFullPath);
            logger.info("Updated: " + file.getName());
        }
        return file;
    }

    public static String findComposerFromPartial(String partialComposer) {
        var composer = "`missing`";
        Trie<String, String> composerTrie = getComposerTrie();
        SortedMap<String, String> prefixMap = composerTrie.prefixMap(partialComposer);
        for (Map.Entry<String, String> entry : prefixMap.entrySet()) {
            composer = entry.getValue();
            logger.debug("Matched composer to: " + composer);
        }
        return composer;
    }
}
