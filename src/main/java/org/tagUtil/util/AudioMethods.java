package org.tagUtil.util;

import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;

import java.io.File;
import java.io.IOException;

public class AudioMethods {

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

        FileHelper.renameFile(file, tag.getFirst(FieldKey.TRACK) + " - " + outputTitle + "." + Format.FLAC);
    }
}
