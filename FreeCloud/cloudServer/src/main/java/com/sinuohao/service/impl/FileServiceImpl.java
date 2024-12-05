package com.sinuohao.service.impl;

import com.sinuohao.config.FileStorageProperties;
import com.sinuohao.model.FileInfo;
import com.sinuohao.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileStorageProperties storageProperties;

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

            // Create user specified directory path if it doesn't exist
            Path userPath = rootLocation.resolve(filepath).normalize();
            
            // Security check - make sure the resolved path is still under root location
            if (!userPath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot store file outside root directory");
            }
            
            Files.createDirectories(userPath);

            // Check if file exists at the specified filepath
            if (Files.exists(userPath)) {
                // Append timestamp to filepath
                String timestamp = String.valueOf(System.currentTimeMillis());
                filepath = filepath + timestamp;
                userPath = rootLocation.resolve(filepath).normalize();
            }

            // Copy file to destination
            Files.copy(file.getInputStream(), userPath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path from base directory
            return filepath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource downloadFile(String filepath, String filename) {
        try {
            // Combine filepath and filename, normalize to prevent path traversal
            Path fullPath = rootLocation.resolve(filepath).resolve(filename).normalize();
            
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
            Path filePath = rootLocation.resolve(path);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + path);
            }

            boolean isDir = Files.isDirectory(filePath);
            return FileInfo.builder()
                    .name(filePath.getFileName().toString())
                    .path(path)
                    .size(isDir ? -1L : Files.size(filePath))
                    .suffix(isDir ? null : getFileExtension(filePath.toString()))
                    .createTime(Instant.ofEpochMilli(Files.getAttribute(filePath, "creationTime").hashCode()))
                    .updateTime(Instant.ofEpochMilli(Files.getLastModifiedTime(filePath).toMillis()))
                    .isDirectory(isDir)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file info", e);
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