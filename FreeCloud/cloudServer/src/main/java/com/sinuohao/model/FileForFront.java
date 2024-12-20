package com.sinuohao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Used to return file info to front, hide the storage detail s3 key
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileForFront {
    private Long id;
    private String name;
    private Long size;
    private String suffix;
    private String type;
    private String createTime;
    private String updateTime;

    public static FileForFront fromFileInfo(FileInfo fileInfo) {
        return FileForFront.builder()
                .id(fileInfo.getId())
                .name(fileInfo.getName())
                .suffix(fileInfo.getSuffix())
                .type(fileInfo.getType())
                .size(fileInfo.getSize())
                .createTime(fileInfo.getCreateTime().toString())
                .updateTime(fileInfo.getUpdateTime().toString())
                .build();
    }
}
