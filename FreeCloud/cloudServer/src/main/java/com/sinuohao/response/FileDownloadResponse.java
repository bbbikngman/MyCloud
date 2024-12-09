package com.sinuohao.response;

import org.springframework.core.io.Resource;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDownloadResponse {
    private Resource originalFile;
    private Resource thumbnail;
    private String error;
    private boolean success;

    public static FileDownloadResponse createError(String errorMessage) {
        return FileDownloadResponse.builder()
                .error(errorMessage)
                .success(false)
                .build();
    }

    public static FileDownloadResponse createSuccess(Resource originalFile, Resource thumbnail) {
        return FileDownloadResponse.builder()
                .originalFile(originalFile)
                .thumbnail(thumbnail)
                .success(true)
                .build();
    }
}
