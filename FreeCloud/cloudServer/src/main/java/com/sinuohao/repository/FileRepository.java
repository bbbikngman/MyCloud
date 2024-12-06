package com.sinuohao.repository;

import com.sinuohao.model.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileInfo, Long> {
    FileInfo findByPathAndNameAndSuffix(String path, String name, String suffix);
    // Add custom queries if needed
}
