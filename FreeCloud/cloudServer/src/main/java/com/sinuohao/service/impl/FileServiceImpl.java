package com.sinuohao.service.impl;

import com.sinuohao.config.FileStorageProperties;
import com.sinuohao.model.FileInfo;
import com.sinuohao.repository.FileRepository;
import com.sinuohao.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {
    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileStorageProperties storageProperties;

    @Autowired
    private FileRepository fileRepository;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(storageProperties.getBasePath());
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String storeFile(String filepath, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }
            if (file.getSize() > storageProperties.getMaxSize()) {
                throw new RuntimeException("File size exceeds maximum limit");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? getFileExtension(originalFilename) : "";
            String baseName = originalFilename != null ? 
                    originalFilename.substring(0, originalFilename.lastIndexOf('.')) : 
                    String.valueOf(System.currentTimeMillis());
            
            // Create user specified directory path if it doesn't exist
            Path directoryPath = rootLocation.resolve(filepath).normalize();
            
            // Security check - make sure the resolved path is still under root location
            if (!directoryPath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside root directory");
            }
            
            Files.createDirectories(directoryPath);

            // Generate unique filename if a file with the same name exists
            String finalFilename = baseName;
            Path finalPath = directoryPath.resolve(finalFilename + "." + extension);
            int counter = 1;
            while (Files.exists(finalPath)) {
                finalFilename = baseName + "_" + counter++;
                finalPath = directoryPath.resolve(finalFilename + "." + extension);
            }

            // Copy file to destination
            Files.copy(file.getInputStream(), finalPath, StandardCopyOption.REPLACE_EXISTING);

            // Save file information to database
            FileInfo fileInfo = FileInfo.builder()
                    .name(finalFilename)
                    .path(filepath)
                    .size(file.getSize())
                    .suffix(extension)
                    .isDirectory(false)
                    .build();
            
            fileRepository.save(fileInfo);

            // Return relative path from base directory
            return filepath + (filepath.endsWith("/") ? "" : "/") + finalFilename + "." + extension;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource downloadFile(String filepath, String filename) {
        try {
            // First try to find the file in database
            int lastDotIndex = filename.lastIndexOf('.');
            String name = lastDotIndex > -1 ? filename.substring(0, lastDotIndex) : filename;
            String suffix = lastDotIndex > -1 ? filename.substring(lastDotIndex + 1) : "";
            
            FileInfo fileInfo = fileRepository.findByPathAndNameAndSuffix(filepath, name, suffix);
            if (fileInfo == null) {
                throw new RuntimeException("File not found in database: " + filepath + "/" + filename);
            }
            
            // Combine filepath and filename, normalize to prevent path traversal
            Path fullPath = rootLocation.resolve(filepath).resolve(name + "." + suffix).normalize();
            
            // Security check - make sure the resolved path is still under root location
            if (!fullPath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Access to file outside root directory is not allowed");
            }
    
            Resource resource = new UrlResource(fullPath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filepath + "/" + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filepath + "/" + filename, e);
        }
    }
    

    @Override
    public void deleteFile(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
            
            // Try to delete the type directory if it's empty
            Path typeDir = file.getParent();
            if (Files.isDirectory(typeDir) && isDirEmpty(typeDir)) {
                Files.delete(typeDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "other";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private boolean isDirEmpty(Path path) throws IOException {
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
            return !directory.iterator().hasNext();
        }
    }

    @Override
    public FileInfo getFileInfo(String path) {
        try {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("Path cannot be null or empty");
            }

            // Remove leading slash if present
            path = path.startsWith("/") ? path.substring(1) : path;

            log.debug("Getting file info - path: {}", path);

            // Split the path into directory and filename
            int lastDotIndex = path.lastIndexOf('.');
            int lastSlashIndex = path.lastIndexOf('/');
            
            String directory = lastSlashIndex > 0 ? path.substring(0, lastSlashIndex) : "";
            String filename = lastSlashIndex > 0 ? path.substring(lastSlashIndex + 1) : path;
            String name = lastDotIndex > lastSlashIndex ? filename.substring(0, filename.lastIndexOf('.')) : filename;
            String suffix = lastDotIndex > lastSlashIndex ? filename.substring(filename.lastIndexOf('.') + 1) : "";

            log.debug("Parsed path components - path: {}, directory: {}, filename: {}, name: {}, suffix: {}", 
                     path, directory, filename, name, suffix);

            // Query the database first
            FileInfo fileInfo = fileRepository.findByPathAndNameAndSuffix(directory, name, suffix);
            if (fileInfo != null) {
                log.debug("Found file info in database: {}", fileInfo);
                return fileInfo;
            }

            log.debug("No record found in database, checking filesystem");

            // If not found in database, check filesystem
            Path filePath = rootLocation.resolve(path).normalize();
            
            // Security check
            if (!filePath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                log.error("Security violation: Attempted to access path outside root directory: {}", filePath);
                throw new SecurityException("Access to file outside root directory is not allowed: " + path);
            }

            if (!Files.exists(filePath)) {
                log.debug("File not found in filesystem: {}", filePath);
                throw new RuntimeException("File not found: " + path);
            }

            boolean isDir = Files.isDirectory(filePath);
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

            FileInfo newFileInfo = FileInfo.builder()
                    .name(name)
                    .path(directory)
                    .size(isDir ? -1L : Files.size(filePath))
                    .suffix(isDir ? null : suffix)
                    .createTime(attrs.creationTime().toInstant())
                    .updateTime(attrs.lastModifiedTime().toInstant())
                    .isDirectory(isDir)
                    .build();

            log.debug("Created new file info from filesystem: {}", newFileInfo);
            
            // Save to database for future queries
            fileRepository.save(newFileInfo);
            log.debug("Saved new file info to database");

            return newFileInfo;

        } catch (SecurityException e) {
            log.error("Security violation accessing path: {}", path);
            throw e;
        } catch (IOException e) {
            log.error("IO error getting file info for path: {}", path, e);
            throw new RuntimeException("Failed to get file info: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error getting file info for path: {}", path, e);
            throw new RuntimeException("Failed to get file info: " + e.getMessage(), e);
        }
    }
    
    @Override    
    public List<FileInfo> listFiles(String path, int start, int end, String sortBy, boolean ascending, String suffix) {
        try {
            // Handle null or empty path
            final String finalPath = (path == null || path.trim().isEmpty()) ? "" : path;
            
            // Remove leading slash if present
            final String normalizedPath = finalPath.startsWith("/") ? finalPath.substring(1) : finalPath;

            log.debug("Listing files - path: {}, start: {}, end: {}, sortBy: {}, ascending: {}, suffix: {}", 
                     normalizedPath, start, end, sortBy, ascending, suffix);

            Path directoryPath = rootLocation.resolve(normalizedPath).normalize();
            
            // Security check
            if (!directoryPath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                log.error("Security violation: Attempted to list directory outside root: {}", directoryPath);
                throw new SecurityException("Access to directory outside root is not allowed: " + normalizedPath);
            }

            if (!Files.exists(directoryPath)) {
                log.warn("Directory not found: {}", directoryPath);
                return java.util.Collections.singletonList(FileInfo.createError(normalizedPath, "Directory not found: " + normalizedPath));
            }

            if (!Files.isDirectory(directoryPath)) {
                log.warn("Path is not a directory: {}", directoryPath);
                return java.util.Collections.singletonList(FileInfo.createError(normalizedPath, "Path is not a directory: " + normalizedPath));
            }

            // List all files in directory
            List<FileInfo> files = Files.list(directoryPath)
                .filter(p -> {
                    if (suffix != null && !suffix.isEmpty()) {
                        return !Files.isDirectory(p) && p.getFileName().toString().toLowerCase().endsWith("." + suffix.toLowerCase());
                    }
                    return true;
                })
                .map(p -> {
                    try {
                        String fileName = p.getFileName().toString();
                        boolean isDir = Files.isDirectory(p);
                        String fileSuffix = isDir ? null : 
                            (fileName.lastIndexOf(".") > -1 ? 
                                fileName.substring(fileName.lastIndexOf(".") + 1) : "");
                        String name = isDir ? fileName :
                            (fileName.lastIndexOf(".") > -1 ? 
                                fileName.substring(0, fileName.lastIndexOf(".")) : 
                                fileName);

                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);

                        return FileInfo.builder()
                                .name(name)
                                .path(normalizedPath)
                                .size(isDir ? -1L : Files.size(p))
                                .suffix(fileSuffix)
                                .createTime(attrs.creationTime().toInstant())
                                .updateTime(attrs.lastModifiedTime().toInstant())
                                .isDirectory(isDir)
                                .build();
                    } catch (IOException e) {
                        log.error("Error getting file info for: {}", p, e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            // Sort the files
            java.util.Comparator<FileInfo> comparator;
            switch (sortBy.toLowerCase()) {
                case "size":
                    comparator = java.util.Comparator.comparing(FileInfo::getSize);
                    break;
                case "createtime":
                    comparator = java.util.Comparator.comparing(FileInfo::getCreateTime);
                    break;
                case "updatetime":
                    comparator = java.util.Comparator.comparing(FileInfo::getUpdateTime);
                    break;
                default:
                    comparator = java.util.Comparator.comparing(FileInfo::getName);
            }

            if (!ascending) {
                comparator = comparator.reversed();
            }

            files.sort(comparator);

            // Apply pagination
            int fromIndex = Math.min(start, files.size());
            int toIndex = Math.min(end, files.size());
            
            log.debug("Found {} files, returning items from {} to {}", files.size(), fromIndex, toIndex);
            
            return files.subList(fromIndex, toIndex);

        } catch (SecurityException e) {
            log.error("Security violation listing files in path: {}", path);
            throw e;
        } catch (Exception e) {
            log.error("Error listing files in path: {}", path, e);
            throw new RuntimeException("Failed to list files: " + e.getMessage(), e);
        }
    }
}