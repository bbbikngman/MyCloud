package com.sinuohao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

import com.sinuohao.util.FileSuffixUtil;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "files")
public class FileInfo {
    public static final int FILE_TYPE_DIRECTORY = 0;
    public static final int FILE_TYPE_NORMAL = 1;
    public static final int FILE_TYPE_THUMBNAIL = 2;    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private Long size;
    
    @Column
    private String suffix;
    
    @Column(name = "create_time", nullable = false)
    private Instant createTime;
    
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    // File Type, generate by suffix accroding to FileSuffixUtil
    @Column(nullable = false)
    private String type;

    // New fields for s3 object storage, see generateS3Key below
    @Column(name = "s3_key")
    private String s3Key;

    

    // Constructor for error cases
    public static FileInfo createError( String errorMessage) {
        return FileInfo.builder()
                .name(errorMessage)
                .size(-1L)
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .build();
    }
    
    @PrePersist
    protected void onCreate() {
        createTime = Instant.now();
        updateTime = createTime;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = Instant.now();
    }

    public String getDisplaySize() {
        // Add additional logic here if needed
        return String.valueOf(size);
    }

    // Helper method to generate S3 key
   public void generateS3Key() {
    // Generate a unique key for the file
        this.s3Key = type + "/" + UUID.randomUUID() + "_" + name + (suffix != null ? "." + suffix : "");
    }
    // Generate content type
    public void generatecontentType() {
        this.type = FileSuffixUtil.getTypeDirectory(this.suffix);
    }
    public void generatecontentTypeAndS3Key() {
        generatecontentType();
        generateS3Key();
    }
}
