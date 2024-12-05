package com.sinuohao.model;

import lombok.Builder;
import lombok.Data;
import javax.persistence.*;
import java.time.Instant;

@Data
@Builder
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
