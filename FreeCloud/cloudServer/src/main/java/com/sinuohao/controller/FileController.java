package com.sinuohao.controller;

import com.sinuohao.model.FileForFront;
import com.sinuohao.model.FileInfo;
import com.sinuohao.response.ApiResponse;
import com.sinuohao.service.FileService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Long>> uploadFile(@RequestParam("file") MultipartFile file) {
        Long id = fileService.storeFile(file);
        return ResponseEntity.ok(ApiResponse.success(id));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        FileInfo fileInfo = fileService.getFileInfoById(id);
        Resource file = fileService.downloadFile(id, fileInfo.getS3Key());
        
        String contentType = fileInfo.getType() + "/" + fileInfo.getSuffix();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + fileInfo.getName() + "." + fileInfo.getSuffix() + "\"")
                .body(file);
    }
   
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileForFront>> getFileInfo(@PathVariable Long id) {
        FileInfo fileInfo = fileService.getFileInfoById(id);
        FileForFront fileForFront = FileForFront.fromFileInfo(fileInfo);
        return ResponseEntity.ok(ApiResponse.success(fileForFront));
    }
    
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<FileForFront>>> listFiles(@RequestParam(value="sortBy", defaultValue="name" ) String sortBy,
            @RequestParam(value="ascending", defaultValue="true") boolean ascending,   
            @RequestParam("name") String name,   
            @RequestParam("suffix") String suffix, 
            @RequestParam("type") String type,
            @RequestParam(value="start", required=false, defaultValue="0") int start,
            @RequestParam(value="end", required=false, defaultValue="100") int end) { 
        List<FileInfo> fileInfos = fileService.listFiles(start, end, sortBy, ascending, name, suffix, type);
        List<FileForFront> files = fileInfos.stream()
                .map(fileInfo -> FileForFront.fromFileInfo(fileInfo))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(files));
    }
}
