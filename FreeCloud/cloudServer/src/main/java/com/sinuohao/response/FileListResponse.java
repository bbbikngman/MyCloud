package com.sinuohao.response;

import com.sinuohao.model.FileInfo;
import org.springframework.core.io.Resource;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileListResponse {
    private List<FileInfo> files;
    private Map<String, Resource> thumbnails; // key is file path
    private String error;
    private boolean success;

    public static FileListResponse createError(String errorMessage) {
        return FileListResponse.builder()
                .files(Collections.emptyList())
                .thumbnails(Collections.emptyMap())
                .error(errorMessage)
                .success(false)
                .build();
    }

    public static FileListResponse createSuccess(List<FileInfo> files, Map<String, Resource> thumbnails) {
        return FileListResponse.builder()
                .files(files)
                .thumbnails(thumbnails)
                .success(true)
                .build();
    }
}
