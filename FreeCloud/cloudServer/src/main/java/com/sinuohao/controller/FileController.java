package com.sinuohao.controller;

import com.sinuohao.model.FileInfo;
import com.sinuohao.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @PostMapping("/upload/**")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            // Extract the directory path from the request URL
            String requestPath = request.getRequestURI();
            String uploadPath = requestPath.substring(requestPath.indexOf("/upload/") + "/upload/".length());
            
            // If no path is specified, use empty string (root directory)
            String filepath = uploadPath.isEmpty() ? "" : uploadPath;
            
            String savedFilePath = fileService.storeFile(filepath, file);
            return ResponseEntity.ok(Map.of("filePath", savedFilePath));
        } catch (Exception e) {
            logger.error("Error uploading file to path: {}", request.getRequestURI(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/download/**")
    public ResponseEntity<?> downloadFile(HttpServletRequest request) {
        try {
            // Extract the full path from the request URL
            String requestPath = request.getRequestURI();
            String downloadPath = requestPath.substring(requestPath.indexOf("/download/") + "/download/".length());
            
            // Split into path and filename
            int lastSlashIndex = downloadPath.lastIndexOf('/');
            String filepath = lastSlashIndex > 0 ? downloadPath.substring(0, lastSlashIndex) : "";
            String filename = lastSlashIndex > 0 ? downloadPath.substring(lastSlashIndex + 1) : downloadPath;
            
            Resource resource = fileService.downloadFile(filepath, filename);
            
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file from path: {}", request.getRequestURI(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{*path}")
    public ResponseEntity<FileInfo> getFileInfo(@PathVariable String path) {
        try {
            FileInfo fileInfo = fileService.getFileInfo(path);
            return ResponseEntity.ok(fileInfo);
        } catch (Exception e) {
            logger.error("Error getting file info for path: {}", path, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(FileInfo.createError(path, e.getMessage()));
        }
    }

    @GetMapping("/list/{*path}")
    public ResponseEntity<?> listFiles(
            @PathVariable String path,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "100") int end,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String suffix) {
        try {
            List<FileInfo> files = fileService.listFiles(path, start, end, sortBy, ascending, suffix);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error listing files for path: {}", path, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonList(FileInfo.createError(path, e.getMessage())));
        }
    }
}
