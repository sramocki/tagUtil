package org.tagUtil.type;

import org.apache.commons.lang3.ArrayUtils;
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
import org.tagUtil.constants.Format;
import org.tagUtil.service.AudioMethods;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OldMusic {

    private static final Logger logger = LogManager.getLogger(OldMusic.class);
    private static final Pattern composerPattern = Pattern.compile("[0-9,()]");

    public static void loopDirectory(File parentFolder) {
        if (parentFolder == null) return;
        var folders = parentFolder.listFiles();


        assert folders != null;
        var startTime = System.currentTimeMillis();
        for (File folder : folders) {
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
        var endTime = System.currentTimeMillis();
        logger.info("Total operation time: " + (endTime - startTime)/1000.0 + " seconds");
    }

    public static void processFolder(File folder) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        var startTime = System.currentTimeMillis();
        var files = folder.listFiles();
        assert files != null;

        Set<String> composerSet = new HashSet<>();
        int missingComposerCount = 0;
        Set<String> orchestraSet = new HashSet<>();

        var audioFileList = Arrays
                .stream(files)
                .filter(Format::isAudio)
                .collect(Collectors.toList());

        if (isNotValidGenre(audioFileList.get(0))) return;

        var albumArtistList = Arrays.asList(getAlbumArtists(audioFileList.get(0)));

        for (var file: audioFileList) {

            var currentComposer = getFieldKey(file, FieldKey.COMPOSER);
            if (StringUtils.isEmpty(currentComposer)) {
                missingComposerCount++;
            } else {
                composerSet.add(currentComposer);
            }
            orchestraSet.add(getFieldKey(file, FieldKey.ORCHESTRA));
        }

        if (missingComposerCount > 0 || albumArtistList.size() <= 2 && albumArtistList.containsAll(composerSet) || albumArtistList.containsAll(composerSet) && !albumArtistList.containsAll(orchestraSet)) {
            logger.info("Skipping updating album artists on " + folder.getName());
            for(int i = 0; i < audioFileList.size(); i++) {
                audioFileList.set(i, AudioMethods.cleanUpAudioFile(audioFileList.get(i)));
            }
            return;
        }

        albumArtistList = albumArtistList
                .stream()
                .filter(e -> !e.isEmpty())
                .filter(e -> !orchestraSet.contains(e))
                .filter(e -> !composerSet.contains(e))
                .collect(Collectors.toList());

        if (composerSet.size() < 4 && isValidComposerList(composerSet)) {
            albumArtistList.addAll(composerSet);
        }

        logger.debug(composerSet.size());
        logger.info("Writing the artists: " + String.join(", ", albumArtistList) + " to ["  + folder.getName() + "]");

        for(File file : audioFileList) {
           correctTags(file, albumArtistList.toArray(String[]::new));
        }

        var endTime = System.currentTimeMillis();
        logger.info("Folder [" + folder.getName() + "] operation time: " + (endTime - startTime)/1000.0 + " seconds");
    }

    private static String[] getAlbumArtists(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();
        return tag.getFields(FieldKey.ALBUM_ARTIST)
                .stream()
                .map(TagField::toString)
                .distinct()
                .toArray(String[]::new);
    }

    private static String getFieldKey(File file, FieldKey fieldKey) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();
        return tag.getFirst(fieldKey);
    }

    private static boolean isValidComposerList(Set<String> list) {
        if (list.isEmpty()) {
            return false;
        } else {
            for (var composer: list) {
                if (!isValidComposer(composer)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isValidComposer(String composer) {
        var status = true;
        if (composer.isEmpty()) {
            status = false;
        } else if (composerPattern.matcher(composer).find()) {
            status = false;
        }
        return status;
    }

    private static boolean isNotValidGenre(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();
        List<String> genreList = Arrays.asList("Anime", "Blues", "Christmas", "Comedy", "Electronic", "Folk/Country", "Halloween", "Hip-Hop", "Jazz", "Jazz/Bebop", "Jazz/Big band", "March", "Mariachi", "Pop/Rock", "R&B", "Reggae", "Soundtrack");
        var genre = tag.getFirst(FieldKey.GENRE);
        return genreList.contains(genre);
    }

    private static void correctTags(File file, String[] albumArtistArray) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        file = AudioMethods.cleanUpAudioFile(file);

        var audioFile = AudioFileIO.read(file);
        FlacTag tag = (FlacTag) audioFile.getTag();

        if (ArrayUtils.isNotEmpty(albumArtistArray)) {
            tag.deleteField(FieldKey.ALBUM_ARTIST);

            for (String val : albumArtistArray) {
                tag.addField(FieldKey.ALBUM_ARTIST, val);
                audioFile.commit();
            }
        }
    }
}
