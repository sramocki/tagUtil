package org.tagUtil.util;

import org.apache.commons.collections4.Trie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.SortedMap;

import static org.tagUtil.Window.getComposerTrie;

public class TagHelper {

    private static final Logger logger = LogManager.getLogger(TagHelper.class);

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
