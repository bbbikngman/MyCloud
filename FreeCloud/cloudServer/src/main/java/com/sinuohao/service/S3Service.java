package com.sinuohao.service;

import java.io.InputStream;

/**
 * Service interface for AWS S3 operations
 */
public interface S3Service {
    /**
     * Upload a file to S3 bucket
     * @param bucketName the name of the bucket
     * @param key the S3 object key
     * @param inputStream the file input stream
     */
    void uploadFile(String bucketName, String key, InputStream inputStream);

    /**
     * Download a file from S3 bucket
     * @param bucketName the name of the bucket
     * @param key the S3 object key
     * @return input stream of the file
     */
    InputStream downloadFile(String bucketName, String key);

    /**
     * Delete a file from S3 bucket
     * @param bucketName the name of the bucket
     * @param key the S3 object key
     */
    void deleteFile(String bucketName, String key);

    /**
     * Check if a file exists in S3 bucket
     * @param bucketName the name of the bucket
     * @param key the S3 object key
     * @return true if file exists, false otherwise
     */
    boolean doesFileExist(String bucketName, String key);
}
