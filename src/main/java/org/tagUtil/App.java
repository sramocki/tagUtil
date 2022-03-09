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
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import javax.swing.JFrame;
import java.io.File;
import java.io.IOException;

//TODO
/**
 *
 * Update files vs process new option
 * old files update album artist to be composer / conductor only (handle case with multi composers)
 * Multiple discs
 * Error reporting for missing tags
 * Handle A-Z structure for output
 * https://musicbrainz.org/doc/MusicBrainz_API
 */

public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) throws CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        logger.info("Starting...");
        var startTime = System.currentTimeMillis();

        // Create GUI
        JFrame frame = new Window("tagUtil");
        frame.setVisible(true);

        // Disables the default logging behavior of jaudiotagger
        java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
        try {
            manager.readConfiguration(App.class.getClassLoader().getResourceAsStream("audioLogger.config"));
        } catch (Exception e) {
            logger.error("Unable to configure audio logging, exiting...", e);
            return;
        }

        var endTime = System.currentTimeMillis();
        logger.info("Finished in " + (endTime - startTime) + " ms");
    }
}