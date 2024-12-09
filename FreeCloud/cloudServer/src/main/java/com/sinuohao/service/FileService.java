package com.sinuohao.service;

import com.sinuohao.model.FileInfo;
import com.sinuohao.response.FileDownloadResponse;
import com.sinuohao.response.FileInfoResponse;
import com.sinuohao.response.FileListResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileService interface provides methods for file operations.
 */
public interface FileService {
    /**
     * Stores a file in the specified filepath.
     * 
     * @param filepath the path where the file will be stored
     * @param file     the file to be stored
     * @return the stored file path
     */
    String storeFile(String filepath, MultipartFile file);

    /**
     * Downloads a file from the specified filepath.
     * 
     * @param filepath the path where the file is stored
     * @param filename the name of the file to be downloaded
     * @return the downloaded file
     */
    FileDownloadResponse downloadFile(String filepath, String filename);

    /**
     * Deletes a file with the specified filename.
     * 
     * @param filename the name of the file to be deleted
     */
    void deleteFile(String filename);

    /**
     * Retrieves information about a file at the specified path.
     * 
     * @param path the path of the file
     * @return the file information
     */
    FileInfoResponse getFileInfo(String path);

    /**
     * Lists files in the specified path with pagination and sorting.
     * 
     * @param path      the path where the files are stored
     * @param start     the start index of the pagination
     * @param end       the end index of the pagination
     * @param sortBy    the field to sort by
     * @param ascending whether to sort in ascending order
     * @param suffix    the file suffix to filter by
     * @return the list of files
     */
    FileListResponse listFiles(String path, int start, int end, String sortBy, boolean ascending, String suffix);
}