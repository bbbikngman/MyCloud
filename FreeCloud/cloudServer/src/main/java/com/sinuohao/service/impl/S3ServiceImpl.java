package com.sinuohao.service.impl;

import com.sinuohao.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;

@Service
public class S3ServiceImpl implements S3Service {
    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    @Autowired
    private S3Client s3Client;

    @Override
    public void uploadFile(String bucketName, String key, InputStream inputStream) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));
            log.info("Successfully uploaded file to S3: bucket={}, key={}", bucketName, key);
        } catch (IOException e) {
            log.error("Failed to upload file to S3: bucket={}, key={}", bucketName, key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public InputStream downloadFile(String bucketName, String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (S3Exception e) {
            log.error("Failed to download file from S3: bucket={}, key={}", bucketName, key, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Override
    public void deleteFile(String bucketName, String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from S3: bucket={}, key={}", bucketName, key);
        } catch (S3Exception e) {
            log.error("Failed to delete file from S3: bucket={}, key={}", bucketName, key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    @Override
    public boolean doesFileExist(String bucketName, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking if file exists in S3: bucket={}, key={}", bucketName, key, e);
            throw new RuntimeException("Failed to check if file exists in S3", e);
        }
    }
}
