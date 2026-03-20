package com.jf.mcp.poi.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class FileAccessValidator
{
    private final FileAccessProperties fileAccessProperties;

    public FileAccessValidator(FileAccessProperties fileAccessProperties)
    {
        this.fileAccessProperties = fileAccessProperties;
    }

    public Path validateReadableFile(String filePath) throws IOException
    {
        if (filePath == null || filePath.isBlank())
        {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        Path allowedRoot = fileAccessProperties.getAllowedRootPath();
        Path candidate = Path.of(filePath).toAbsolutePath().normalize();
        if (!candidate.startsWith(allowedRoot))
        {
            throw new IllegalArgumentException("禁止访问授权目录之外的文件");
        }
        if (!Files.isRegularFile(candidate))
        {
            throw new IllegalArgumentException("文件不存在或不是普通文件");
        }

        Path realPath = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (!realPath.startsWith(allowedRoot))
        {
            throw new IllegalArgumentException("禁止通过链接访问授权目录之外的文件");
        }
        return realPath;
    }
}
