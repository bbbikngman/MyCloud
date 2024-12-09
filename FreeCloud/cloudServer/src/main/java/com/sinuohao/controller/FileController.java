package com.sinuohao.controller;

import com.sinuohao.model.FileInfo;
import com.sinuohao.service.FileService;



import com.sinuohao.response.*;
import com.sinuohao.util.FileUtil;

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

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;



@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @PostMapping("/upload/**")
    public ResponseEntity<FileInfoResponse> uploadFile( HttpServletRequest request,
                                                     @RequestParam("file") MultipartFile file) {
        String filepath = "";
        try {
            // Extract filepath from URI
            filepath = FileUtil.extractAndSanitizePath(request.getRequestURI(), "upload");
           
            String path = fileService.storeFile(filepath, file);
            FileInfoResponse response = fileService.getFileInfo(path);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading file: {}", file.getOriginalFilename(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileInfoResponse.createError(filepath, e.getMessage()));
        }
    }

    @GetMapping("/download/**")
    public ResponseEntity<?> downloadFile(HttpServletRequest request) {
        String downloadPath = FileUtil.extractAndSanitizePath(request.getRequestURI(), "download");
        try {
            if (downloadPath.contains("/download/")) {
                downloadPath = downloadPath.substring(downloadPath.indexOf("/download/") + "/download/".length());
            }
            int lastSlashIndex = downloadPath.lastIndexOf('/');
            String filepath = lastSlashIndex > 0 ? downloadPath.substring(0, lastSlashIndex) : "";
            String filename = lastSlashIndex > 0 ? downloadPath.substring(lastSlashIndex + 1) : downloadPath;
            
            FileDownloadResponse downloadResponse = fileService.downloadFile(filepath, filename);
            
            if (!downloadResponse.isSuccess()) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(downloadResponse);
            }

            Resource originalFile = downloadResponse.getOriginalFile();
            String contentType = request.getServletContext().getMimeType(originalFile.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFile.getFilename() + "\"")
                    .body(originalFile);
        } catch (Exception e) {
            logger.error("Error downloading file from path: {}", downloadPath, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileDownloadResponse.createError(e.getMessage()));
        }
    }

    @GetMapping("/info/**")
    public ResponseEntity<FileInfoResponse> getFileInfo(HttpServletRequest request) {
        String path = FileUtil.extractAndSanitizePath(request.getRequestURI(), "info");
        try {
            FileInfoResponse response = fileService.getFileInfo(path);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting file info for path: {}", path, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileInfoResponse.createError(path, e.getMessage()));
        }
    }

    @DeleteMapping("/delete/**")
    public ResponseEntity<FileInfoResponse> deleteFile(HttpServletRequest request) {
        String filename = FileUtil.extractAndSanitizePath(request.getRequestURI(), "delete");

        try {
            fileService.deleteFile(filename);
            return ResponseEntity.ok(FileInfoResponse.createSuccess(null, null));
        } catch (Exception e) {
            logger.error("Error deleting file: {}", filename, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileInfoResponse.createError(filename, e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<FileListResponse> listFiles(
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "100") int end,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String suffix) {
        try {
            FileListResponse response = fileService.listFiles(path, start, end, sortBy, ascending, suffix);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error listing files in path: {}", path, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FileListResponse.createError(e.getMessage()));
        }
    }

    
    
}
