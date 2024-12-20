package com.sinuohao.util;

import java.util.HashMap;
import java.util.Map;

public class FileSuffixUtil {
    public enum FileType {
        IMAGES("image"),
        VIDEOS("video"),
        AUDIO("audio"),
        CODE("code"),
        TEXT("text"),
        SPREADSHEETS("spreadsheets"),
        PRESENTATIONS("presentations"),
        DOCUMENTS("documents"),
        ARCHIVES("archives"),
        OTHERS("others");

        private final String directory;

        FileType(String directory) {
            this.directory = directory;
        }

        public String getDirectory() {
            return directory;
        }
    }

    private static final Map<String, FileType> suffixToType = new HashMap<>();

    static {
        // Images
        addMapping("jpg,jpeg,png,gif,bmp,webp,svg,ico", FileType.IMAGES);

        // Videos
        addMapping("mp4,avi,mkv,mov,wmv,flv,webm", FileType.VIDEOS);

        // Audio
        addMapping("mp3,wav,ogg,aac,wma,m4a", FileType.AUDIO);

        // Code files
        addMapping("java,py,js,cpp,c,h,cs,php,rb,go,swift,kt,ts,html,css,sql", FileType.CODE);

        // Text files
        addMapping("txt,log,md,json,xml,yaml,yml,ini,conf,properties,md", FileType.TEXT);

        // Spreadsheets
        addMapping("xlsx,xls,csv,ods", FileType.SPREADSHEETS);

        // Presentations
        addMapping("ppt,pptx,odp,key", FileType.PRESENTATIONS);

        // Documents
        addMapping("doc,docx,pdf,odt,rtf", FileType.DOCUMENTS);

        // Archives
        addMapping("zip,rar,7z,tar,gz,bz2", FileType.ARCHIVES);
    }

    private static void addMapping(String suffixes, FileType type) {
        for (String suffix : suffixes.split(",")) {
            suffixToType.put(suffix.trim(), type);
        }
    }

    public static FileType getType(String suffix) {
        if (suffix == null) return FileType.OTHERS;
        return suffixToType.getOrDefault(suffix.toLowerCase(), FileType.OTHERS);
    }

    public static String getTypeDirectory(String suffix) {
        return getType(suffix).getDirectory();
    }
}
