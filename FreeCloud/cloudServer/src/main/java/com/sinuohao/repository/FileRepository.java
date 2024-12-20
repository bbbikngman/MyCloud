package com.sinuohao.repository;

import com.sinuohao.model.FileInfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileInfo, Long> {
    FileInfo findByPathAndNameAndSuffix(String path, String name, String suffix);
    List<FileInfo> findByPath(String path);
    // Add custom queries if needed

    @Query("SELECT f FROM FileInfo f WHERE " +
           "(:name IS NULL OR f.name LIKE %:name%) AND " +
           "(:suffix IS NULL OR f.suffix = :suffix) AND " +
           "(:type IS NULL OR f.type = :type)")
    List<FileInfo> findByFilters(
            @Param("name") String name,
            @Param("suffix") String suffix,
            @Param("type") String type);
}
