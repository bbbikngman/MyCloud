package com.sinuohao.controller;

import com.sinuohao.model.FileInfo;
import com.sinuohao.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload/**")
    public ResponseEntity<Map<String, String>> uploadFile(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request) {
    // Extract the directory path from the request URL
    String requestPath = request.getRequestURI();
    String uploadPath = requestPath.substring(requestPath.indexOf("/upload/") + "/upload/".length());
    
    // If no path is specified, use empty string (root directory)
    String filepath = uploadPath.isEmpty() ? "" : uploadPath;
    
    String savedFilePath = fileService.storeFile(filepath, file);
    return ResponseEntity.ok(Map.of("filePath", savedFilePath));
}

    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        // Extract the full path from the request URL
        String requestPath = request.getRequestURI();
        String downloadPath = requestPath.substring(requestPath.indexOf("/download/") + "/download/".length());
        
        // Split into path and filename
        int lastSlashIndex = downloadPath.lastIndexOf('/');
        String filepath = lastSlashIndex > 0 ? downloadPath.substring(0, lastSlashIndex) : "";
        String filename = lastSlashIndex > 0 ? downloadPath.substring(lastSlashIndex + 1) : downloadPath;
    
        Resource resource = fileService.downloadFile(filepath, filename);
        
        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Fallback to octet-stream if type cannot be determined
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
    
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/{*path}")
    public ResponseEntity<FileInfo> getFileInfo(@PathVariable String path) {
        FileInfo fileInfo = fileService.getFileInfo(path);
        return ResponseEntity.ok(fileInfo);
    }

    @GetMapping("/list/{*path}")
    public ResponseEntity<List<FileInfo>> listFiles(
            @PathVariable String path,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "100") int end,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending,
            @RequestParam(required = false) String suffix) {
        List<FileInfo> files = fileService.listFiles(path, start, end, sortBy, ascending, suffix);
        return ResponseEntity.ok(files);
    }
}
