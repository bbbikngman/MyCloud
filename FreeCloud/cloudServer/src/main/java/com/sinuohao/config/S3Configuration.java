package com.sinuohao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class S3Configuration  {
    
    @Value("#{aws.s3.bucket}")
    private String defaultBucket;

    @Value("#{aws.s3.region}")
    private String defaultRegion;

    @Bean
    public S3Client S3Client() {
        return S3Client.builder()
            .region(Region.of(defaultRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    /**
     *  create S3 Bucket(if not exist)
     * @param bucketName strore bucket name 
     * @return if crerated return true
     */
    public boolean createBucketIfNotExists(String bucketName) {
        try{
            S3Client s3Client = S3Client();

            // check if the bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
            try {
                s3Client.headBucket(headBucketRequest);
                log.info("Bucket {} already exists", bucketName);
                return true;
            } catch (S3Exception e) {
                // Bucket does not exist, create it
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

                s3Client.createBucket(createBucketRequest);
                log.info("Bucket {} created successfully", bucketName);
                return true;
            }
                
            } catch (Exception e) {
                log.error("Error creating bucket{}: {}", bucketName, e.getMessage());
                return false;
            }
        }
        /**
         * get default bucket name
         * @return default bucket name
         */
        public String getDefaultBucket() {
            return defaultBucket;
        }
}
