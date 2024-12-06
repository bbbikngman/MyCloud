package com.sinuohao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "files")
public class FileInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String path;
    
    @Column
    private Long size;
    
    @Column
    private String suffix;
    
    @Column(name = "create_time", nullable = false)
    private Instant createTime;
    
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;
    
    @Column(name = "is_directory", nullable = false)
    private boolean isDirectory;

    // Constructor for error cases
    public static FileInfo createError(String path, String errorMessage) {
        return FileInfo.builder()
                .name(errorMessage)
                .path(path)
                .size(-1L)
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .isDirectory(false)
                .build();
    }
    
    @PrePersist
    protected void onCreate() {
        createTime = Instant.now();
        updateTime = Instant.now();
        if (isDirectory) {
            size = -1L;
            suffix = null;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = Instant.now();
    }
}
