package org.tagUtil;

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
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey;
import org.tagUtil.util.AudioMethods;
import org.tagUtil.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OldMusic {

    private static final Logger logger = LogManager.getLogger(OldMusic.class);

    public static void loopDirectory(File parentFolder) {
        var folders = parentFolder.listFiles();

        assert folders != null;
        for (File folder : folders)
        {
            try {
                if (AudioMethods.isMultiDiscs(folder)) {
                    //TODO
                } else {
                   processFolder(folder);
                }

            } catch (Exception e) {
                logger.error("Error on: " + folder.getAbsolutePath(), e);
            }
        }
    }

    public static void processFolder(File folder) throws CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        var files = folder.listFiles();
        assert files != null;

        Set<String> composerList = new HashSet<>();
        Set<String> orchestraList = new HashSet<>();
        Set<String> conductorList = new HashSet<>();

        List<File> audioFileList = Arrays
                .stream(files)
                .filter(FileHelper::isAudio)
                .collect(Collectors.toList());

        if (isNotValidGenre(audioFileList.get(0))) return;

        var albumArtistString = getFieldKey(audioFileList.get(0), FieldKey.ALBUM_ARTIST);
        var albumArtistList = Arrays.asList(albumArtistString.split("; "));

        if (albumArtistList.size() == 1) return;

        for (File file: audioFileList) {
            String rawComposerArray = getFieldKey(file, FieldKey.COMPOSER);
            String[] composerArray = rawComposerArray.split("; ");
            composerList.addAll(Arrays.asList(composerArray));

            String rawOrchestraArray = getFieldKey(file, FieldKey.ORCHESTRA);
            String[] orchestraArray = rawOrchestraArray.split("; ");
            orchestraList.addAll(Arrays.asList(orchestraArray));
        }

        albumArtistList = albumArtistList
                .stream()
                .filter(e -> !e.isEmpty())
                .filter(e -> !orchestraList.contains(e))
                .collect(Collectors.toList());

        for(File file : audioFileList) {
           correctTags(file, albumArtistList.toArray(String[]::new));
        }
    }

    private static String getFieldKey(File file, FieldKey fieldKey) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();
        List<TagField> composerList = tag.getFields(fieldKey);
        if (composerList.size() == 1) {
            return composerList.get(0).toString();
        } else {
            return composerList.stream()
                    .map(TagField::toString)
                    .collect(Collectors.joining("; "));
        }
    }

    private static boolean isNotValidGenre(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();
        List<String> genreList = Arrays.asList("Anime", "Blues", "Christmas", "Comedy", "Electronic", "Folk/Country", "Halloween", "Hip-Hop", "Jazz", "Jazz/Bebop", "Jazz/Big band", "March", "Mariachi", "Pop/Rock", "R&B", "Reggae", "Soundtrack");
        var genre = tag.getFirst(FieldKey.GENRE);
        return genreList.contains(genre);
    }

    private static void correctTags(File file, String[] albumArtistArray) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();

        String composer = tag.getFirst(FieldKey.COMPOSER);
        if (StringUtils.isEmpty(composer)) {
            tag.setField(FieldKey.COMPOSER, "`missing`");
            audioFile.commit();
        }

        tag.deleteField(FieldKey.ALBUM_ARTIST);

        for (String val : albumArtistArray) {
            tag.addField(FieldKey.ALBUM_ARTIST, val);
            audioFile.commit();
        }
    }
}
