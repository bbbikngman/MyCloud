package com.sinuohao.service;

import com.sinuohao.model.FileInfo;

import lombok.NonNull;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileService interface provides methods for file operations.
 */
public interface FileService {
    /**
     * Stores a file in s3.
     * 
     * @param file     the file to be stored
     * @return the id of the stored file
     */
    Long storeFile(MultipartFile file);

    /**
     * Downloads a file by s3Key, if it's empty, get s3Key by id first.
     * 
     * @param id the id of the file, not null
     * @param s3Key the s3Key of the file to be downloaded
     * @return the downloaded file Resource
     */
    Resource  downloadFile(@NonNull Long id, String s3Key);

    /**
     * Deletes a file with the specified filename.
     * 
     * @param filename the name of the file to be deleted
     */
    void deleteFile(String filename);

    /**
     * Retrieves information about a file by id
     * 
     * @param id the id of the file
     * @return the file information
     */
    FileInfo getFileInfoById(Long id);
    /**
     * Lists files in the specified path with pagination and sorting.
     * 
     * 
     * @param start     the start index of the pagination
     * @param end       the end index of the pagination
     * @param sortBy    the field to sort by
     * @param ascending whether to sort in ascending order
     * @param name      the files contain this name
     * @param suffix    the file suffix to filter by
     * @paran type      the file type to filter by
     * @return the list of files
     */
    List<FileInfo> listFiles(int start, int end, String sortBy, boolean ascending, String name, String suffix, String type);
}