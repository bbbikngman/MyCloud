package com.sinuohao.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class FileUtil {
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    ));
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    public static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "other";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    public static String getBaseName(String filename) {
        if (filename == null) {
            return String.valueOf(System.currentTimeMillis());
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > -1 ? filename.substring(0, lastDotIndex) : filename;
    }

    public static boolean isImage(String filename) {
        String extension = getFileExtension(filename);
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static Resource generateThumbnail(File file) {
        if (!isImage(file.getName())) {
            return null;
        }

        try {
            // Read original image
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                log.warn("Could not read image file: {}", file.getName());
                return null;
            }

            // Calculate thumbnail dimensions maintaining aspect ratio
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            double scale = Math.min((double) THUMBNAIL_WIDTH / originalWidth, 
                                  (double) THUMBNAIL_HEIGHT / originalHeight);
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            // Create thumbnail
            BufferedImage thumbnailImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnailImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String extension = getFileExtension(file.getName());
            ImageIO.write(thumbnailImage, extension, baos);
            
            return new ByteArrayResource(baos.toByteArray()) {
                @Override
                public String getFilename() {
                    return "thumbnail_" + file.getName();
                }
            };

        } catch (IOException e) {
            log.error("Failed to generate thumbnail for: " + file.getName(), e);
            return null;
        }
    }

    public static PathInfo parseFilePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        // Remove leading slash if present
        path = path.startsWith("/") ? path.substring(1) : path;

        // Split the path into directory and filename
        int lastDotIndex = path.lastIndexOf('.');
        int lastSlashIndex = path.lastIndexOf('/');
        
        String directory = lastSlashIndex > 0 ? path.substring(0, lastSlashIndex) : "";
        String filename = lastSlashIndex > 0 ? path.substring(lastSlashIndex + 1) : path;
        String name = lastDotIndex > lastSlashIndex ? filename.substring(0, filename.lastIndexOf('.')) : filename;
        String suffix = lastDotIndex > lastSlashIndex ? filename.substring(filename.lastIndexOf('.') + 1) : "";

        return new PathInfo(directory, name, suffix);
    }

   private static String extractPathAfterSegment(String uri, String segment) {
        int index = uri.indexOf("/" + segment + "/");
        if (index != -1) {
            String path = uri.substring(index + segment.length() + 2);
            // Optional: add path sanitization here if needed
            return path;
        }
        return "";
    }

    private static String sanitizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // Convert backslashes to forward slashes
        path = path.replace('\\', '/');
        // Remove leading/trailing slashes
        path = path.replaceAll("^/+|/+$", "");
        // Normalize multiple slashes
        path = path.replaceAll("/+", "/");
        // Remove directory traversal attempts
        return Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty() && !".".equals(s) && !"..".equals(s))
                .collect(Collectors.joining("/"));
    }

    // New public method that combines both operations
    public static String extractAndSanitizePath(String uri, String segment) {
        String extractedPath = extractPathAfterSegment(uri, segment);
        return sanitizePath(extractedPath);
    }

    public static class PathInfo {
        private final String directory;
        private final String name;
        private final String suffix;

        public PathInfo(String directory, String name, String suffix) {
            this.directory = directory;
            this.name = name;
            this.suffix = suffix;
        }

        public String getDirectory() { return directory; }
        public String getName() { return name; }
        public String getSuffix() { return suffix; }
    }
}
