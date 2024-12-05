package com.sinuohao.service;

import com.sinuohao.model.FileInfo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import java.util.List;



public interface FileService {
    String storeFile(String filepath, MultipartFile file);  
    Resource downloadFile(String filepath, String filename);  
    void deleteFile(String filename);
    FileInfo getFileInfo(String path);
    List<FileInfo> listFiles(String path, int start, int end, String sortBy, boolean ascending, String suffix);
}