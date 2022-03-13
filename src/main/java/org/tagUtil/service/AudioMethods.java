package org.tagUtil.service;

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
import org.tagUtil.constants.Format;
import org.tagUtil.constants.RegexMatches;
import org.tagUtil.util.FileHelper;
import org.tagUtil.util.TagHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AudioMethods {

    private static final Logger logger = LogManager.getLogger(AudioMethods.class);

    // TODO rewrite
    public static boolean isMultiDiscs(File folder) {
        List<File> fileList = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isDirectory)
                .collect(Collectors.toList());
        return fileList.size() > 1;
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
                missingComposer = TagHelper.findComposerFromPartial(partialComposer.toLowerCase());
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
        String outputTitle = trackTitle.replaceAll(RegexMatches.ILLEGAL_REGEX_DASH, "-").replaceAll(RegexMatches.ILLEGAL_REGEX_BLANK, StringUtils.EMPTY);
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
        } else {
            logger.debug("Skipped: " + file.getName());
        }
        return file;
    }
}
