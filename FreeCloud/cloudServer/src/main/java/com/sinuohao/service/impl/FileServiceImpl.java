package com.sinuohao.service.impl;

import com.sinuohao.config.FileStorageProperties;
import com.sinuohao.model.FileInfo;
import com.sinuohao.repository.FileRepository;
import com.sinuohao.response.FileDownloadResponse;
import com.sinuohao.response.FileInfoResponse;
import com.sinuohao.response.FileListResponse;
import com.sinuohao.service.FileService;
import com.sinuohao.util.FileUtil;
import com.sinuohao.util.FileUtil.PathInfo;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

           // Sanitize the filepath
           filepath = FileUtil.sanitizePath(filepath);

            String originalFilename = file.getOriginalFilename();
            String extension = FileUtil.getFileExtension(originalFilename);
            String baseName = FileUtil.getBaseName(originalFilename);
            
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
    public FileDownloadResponse downloadFile(String filepath, String filename) {
        try {
            FileUtil.sanitizePath(filepath); // sanitizePath

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
                Resource thumbnail = null;
                if (FileUtil.isImage(filename)) {
                    thumbnail = FileUtil.generateThumbnail(fullPath.toFile());
                }
                return FileDownloadResponse.createSuccess(resource, thumbnail);
            } else {
                throw new RuntimeException("Could not read file: " + filepath + "/" + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filepath + "/" + filename, e);
        }
    }

    @Override
    public FileInfoResponse getFileInfo(String path) {
        try {
            FileUtil.sanitizePath(path); // sanitizePath
            
            FileUtil.PathInfo pathInfo = FileUtil.parseFilePath(path);
            log.debug("Parsed path components - directory: {}, name: {}, suffix: {}", 
                     pathInfo.getDirectory(), pathInfo.getName(), pathInfo.getSuffix());

            // Query the database first
            FileInfo fileInfo = fileRepository.findByPathAndNameAndSuffix(
                pathInfo.getDirectory(), pathInfo.getName(), pathInfo.getSuffix());

            Path filePath = rootLocation.resolve(path).normalize();
            
            // Security check
            if (!filePath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Access to file outside root directory is not allowed");
            }

            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found: " + path);
            }

            // Create new FileInfo if not found in database
            if (fileInfo == null) {
                fileInfo = FileInfo.builder()
                        .name(pathInfo.getName())
                        .path(pathInfo.getDirectory())
                        .size(Files.size(filePath))
                        .suffix(pathInfo.getSuffix())
                        .isDirectory(Files.isDirectory(filePath))
                        .build();
                fileRepository.save(fileInfo);
            }

            // Generate thumbnail if it's an image
            Resource thumbnail = null;
            if (FileUtil.isImage(path)) {
                thumbnail = FileUtil.generateThumbnail(filePath.toFile());
            }

            return FileInfoResponse.createSuccess(fileInfo, thumbnail);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file info", e);
        }
    }

    @Override
    public FileListResponse listFiles(String path, int start, int end, String sortBy, boolean ascending, String suffix) {
        try {
            // Sanitize the path
            path = FileUtil.sanitizePath(path);
            
            Path directoryPath = rootLocation.resolve(path).normalize();
            
            // Security check
            if (!directoryPath.toAbsolutePath().startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("Cannot access directory outside root directory");
            }
            
            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                throw new RuntimeException("Directory not found: " + path);
            }

            List<FileInfo> files = fileRepository.findByPath(path);
            if (files.isEmpty()) {
                // If not in database, scan directory
                String localPath = path;
                files = Files.list(directoryPath)
                    .map(p -> {
                        try {
                            String fileName = p.getFileName().toString();
                            String fileSuffix = FileUtil.getFileExtension(fileName);
                            String baseName = FileUtil.getBaseName(fileName);
                            
                            return FileInfo.builder()
                                    .name(baseName)
                                    .path(localPath)
                                    .size(Files.isDirectory(p) ? -1L : Files.size(p))
                                    .suffix(fileSuffix)
                                    .isDirectory(Files.isDirectory(p))
                                    .build();
                        } catch (IOException e) {
                            log.error("Error reading file attributes: " + p, e);
                            return null;
                        }
                    })
                    .filter(f -> f != null)
                    .collect(Collectors.toList());
                
                // Save to database
                fileRepository.saveAll(files);
            }

            // Apply filtering and sorting
            files = files.stream()
                .filter(f -> suffix == null || suffix.isEmpty() || f.getSuffix().equals(suffix))
                .sorted((f1, f2) -> {
                    int result = 0;
                    switch (sortBy) {
                        case "name":
                            result = f1.getName().compareTo(f2.getName());
                            break;
                        case "size":
                            result = Long.compare(f1.getSize(), f2.getSize());
                            break;
                        case "time":
                            result = f1.getCreateTime().compareTo(f2.getCreateTime());
                            break;
                        default:
                            result = f1.getName().compareTo(f2.getName());
                    }
                    return ascending ? result : -result;
                })
                .collect(Collectors.toList());

            // Apply pagination
            int toIndex = Math.min(end, files.size());
            files = files.subList(Math.min(start, files.size()), toIndex);

            // Generate thumbnails for images
            Map<String, Resource> thumbnails = new HashMap<>();
            for (FileInfo file : files) {
                if (FileUtil.isImage(file.getName() + "." + file.getSuffix())) {
                    Path filePath = rootLocation.resolve(file.getPath())
                            .resolve(file.getName() + "." + file.getSuffix());
                    Resource thumbnail = FileUtil.generateThumbnail(filePath.toFile());
                    if (thumbnail != null) {
                        thumbnails.put(file.getPath() + "/" + file.getName() + "." + file.getSuffix(), thumbnail);
                    }
                }
            }

            return FileListResponse.createSuccess(files, thumbnails);
        } catch (IOException e) {
            throw new RuntimeException("Failed to list files", e);
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

    private boolean isDirEmpty(Path path) throws IOException {
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
            return !directory.iterator().hasNext();
        }
    }
}