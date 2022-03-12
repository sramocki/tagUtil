package org.tagUtil;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JFrame;
import java.util.Map;
import java.util.SortedMap;

//TODO
/**
 *
 * Multiple discs
 * Error reporting for missing tags
 * Handle A-Z structure for output
 * https://musicbrainz.org/doc/MusicBrainz_API
 */

public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    public static void main(String[] args) {
        // Create GUI
        JFrame frame = new Window("tagUtil");
        frame.setVisible(true);

        // Disables the default logging behavior of jaudiotagger
        java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();
        try {
            manager.readConfiguration(App.class.getClassLoader().getResourceAsStream("audioLogger.config"));
        } catch (Exception e) {
            logger.error("Unable to configure audio logging, exiting...", e);
        }
    }
}