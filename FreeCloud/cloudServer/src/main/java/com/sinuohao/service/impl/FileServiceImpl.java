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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
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
                throw new SecurityException("Access to file outside root directory is not allowed");
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

        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + path, e);
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
            Path dirPath = rootLocation.resolve(path);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                throw new RuntimeException("Directory not found: " + path);
            }
            
            // Use "name" as default sort if sortBy is empty
            String effectiveSortBy = (sortBy == null || sortBy.trim().isEmpty()) ? "name" : sortBy.toLowerCase();
            
            List<FileInfo> files = Files.list(dirPath)
                .map(p -> {
                    try {
                        boolean isDir = Files.isDirectory(p);
                        String fileSuffix = isDir ? null : getFileExtension(p.toString());
                        // Skip files that don't match the suffix filter
                        if (!isDir && suffix != null && !suffix.isEmpty() && !suffix.equals(fileSuffix)) {
                            return null;
                        }
                        return FileInfo.builder()
                            .name(p.getFileName().toString())
                            .path(path + "/" + p.getFileName())
                            .size(isDir ? -1L : Files.size(p))
                            .suffix(fileSuffix)
                            .createTime(Instant.ofEpochMilli(Files.getAttribute(p, "creationTime").hashCode()))
                            .updateTime(Instant.ofEpochMilli(Files.getLastModifiedTime(p).toMillis()))
                            .isDirectory(isDir)
                            .build();
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to get file info", e);
                    }
                })
                .filter(f -> f != null) // Remove null entries from suffix filtering
                .sorted((f1, f2) -> {
                    int result;
                    switch (effectiveSortBy) {
                        case "name":
                            result = f1.getName().compareTo(f2.getName());
                            break;
                        case "size":
                            result = Long.compare(f1.getSize(), f2.getSize());
                            break;
                        case "suffix":
                            // Handle null suffixes in comparison
                            String suffix1 = f1.getSuffix() != null ? f1.getSuffix() : "";
                            String suffix2 = f2.getSuffix() != null ? f2.getSuffix() : "";
                            result = suffix1.compareTo(suffix2);
                            break;
                        case "lastmodified":
                            result = f1.getUpdateTime().compareTo(f2.getUpdateTime());
                            break;
                        default:
                            result = f1.getName().compareTo(f2.getName());
                    }
                    return ascending ? result : -result;
                })
                .collect(Collectors.toList());

            // Apply pagination
            int totalFiles = files.size();
            int startIndex = Math.min(start, totalFiles);
            int endIndex = Math.min(end, totalFiles);
                
            return files.subList(startIndex, endIndex);
        } catch (IOException e) {
            throw new RuntimeException("Failed to list files", e);
        }
    }
}