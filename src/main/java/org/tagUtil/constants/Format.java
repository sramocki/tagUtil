package org.tagUtil.constants;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class Format {
    public static final String FLAC = "flac";
    public static final String MP3 = "mp3";

    public static boolean isAudio(File file) { return FilenameUtils.isExtension(file.getName().toLowerCase(), FLAC); }

    public static boolean isJunk(File file) { return FilenameUtils.isExtension(file.getName().toLowerCase(), "log", "cue", "m3u", "m3u8"); }
}
