package org.tagUtil.constants;

import java.util.regex.Pattern;

public class RegexMatches {
    public static final String MOVEMENT_PATTERN = "(:.*)[XVI]{1,}";
    public static final String ILLEGAL_REGEX_DASH = "[\\\\/:]";
    public static final String ILLEGAL_REGEX_BLANK = "[\"?<>|*]";
    public static final Pattern FOLDER_PATTERN = Pattern.compile("(frontcover)|(folder)|(cover)|(front)");
    public static final Pattern BACK_PATTERN = Pattern.compile("(backcover)|(back)|(tray)|(bck)|(inlay)|(rear)");
    public static final String FOLDER = "folder.jpg";
    public static final String BACK = "back.jpg";
}
