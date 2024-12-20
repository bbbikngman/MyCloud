package com.sinuohao.service.impl;

import com.sinuohao.config.FileStorageProperties;
import com.sinuohao.config.S3Configuration;
import com.sinuohao.exception.FileDownloadException;
import com.sinuohao.exception.FileNotReadableException;
import com.sinuohao.model.FileInfo;
import com.sinuohao.repository.FileRepository;
import com.sinuohao.service.FileService;
import com.sinuohao.service.S3Service;
import com.sinuohao.util.FileUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Collections;

/**
 * Service for storing and retrieving files.
 */
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileStorageProperties storageProperties;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private S3Configuration s3Configuration;

    @Autowired
    private S3Service s3Service;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        // Initialize storage location setted in application.yml
        rootLocation = Paths.get(storageProperties.getBasePath());
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public Long storeFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }
            if (file.getSize() > storageProperties.getMaxSize()) {
                throw new RuntimeException("File size exceeds maximum limit");
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = FileUtil.getFileExtension(originalFilename);
            String baseName = FileUtil.getBaseName(originalFilename);

            String bucketName = s3Configuration.getDefaultBucket();
           
           
            // generate file metadata and save to database
            FileInfo fileInfo = FileInfo.builder()
                    .name(baseName)
                    .size(file.getSize())
                    .suffix(extension)
                    .build();
            fileInfo.generatecontentTypeAndS3Key();
            fileRepository.save(fileInfo);

            // store file to S3
            s3Service.uploadFile(bucketName, fileInfo.getS3Key(), file.getInputStream());
            // give the id of the file in case front need to use it immediately
            return fileInfo.getId();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

 @Override
    public Resource downloadFile(Long id, String s3Key) {
        // Don't catch, let it throw FileNotFoundException
        if (s3Key == null || s3Key.isEmpty()) {
            FileInfo fileInfo = getFileInfoById(id);
            s3Key = fileInfo.getS3Key();
        }
        
        try {
            // Convert InputStream to Resource properly
            InputStream inputStream = s3Service.downloadFile(s3Configuration.getDefaultBucket(), s3Key);
            Resource resource = new InputStreamResource(inputStream);
            
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotReadableException("File exists but is not readable: " + s3Key);
            }
            
            return resource;
        } catch (Exception e) {
            throw new FileDownloadException("Failed to download file: " + s3Key, e);
        }
    }

    @Override
    public FileInfo getFileInfoById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found by id: " + id));
    }

    @Override
    public List<FileInfo> listFiles(int start, int end, String sortBy, boolean ascending, String name, String suffix, String type) {
        // Get files with filters from repository
        List<FileInfo> files = fileRepository.findByFilters(
                name.isEmpty() ? null : name,
                suffix.isEmpty() ? null : suffix,
                type.isEmpty() ? null : type);
        
        // Apply sorting
        files.sort((f1, f2) -> {
            int result;
            switch (sortBy.toLowerCase()) {
                case "name":
                    result = f1.getName().compareTo(f2.getName());
                    break;
                case "size":
                    result = Long.compare(f1.getSize(), f2.getSize());
                    break;
                case "createtime":
                    result = f1.getCreateTime().compareTo(f2.getCreateTime());
                    break;
                case "updatetime":
                    result = f1.getUpdateTime().compareTo(f2.getUpdateTime());
                    break;
                default:
                    result = f1.getName().compareTo(f2.getName());
            }
            return ascending ? result : -result;
        });
        
        // Apply pagination
        if (start >= files.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(end, files.size());
        return files.subList(start, toIndex);
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