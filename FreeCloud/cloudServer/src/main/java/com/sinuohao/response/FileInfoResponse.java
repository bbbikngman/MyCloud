package com.sinuohao.response;

import com.sinuohao.model.FileInfo;
import org.springframework.core.io.Resource;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoResponse {
    private FileInfo fileInfo;
    private Resource thumbnail;
    private String error;
    private boolean success;

    public static FileInfoResponse createError(String path, String errorMessage) {
        return FileInfoResponse.builder()
                .fileInfo(FileInfo.builder().path(path).build())
                .error(errorMessage)
                .success(false)
                .build();
    }

    public static FileInfoResponse createSuccess(FileInfo fileInfo, Resource thumbnail) {
        return FileInfoResponse.builder()
                .fileInfo(fileInfo)
                .thumbnail(thumbnail)
                .success(true)
                .build();
    }
}
