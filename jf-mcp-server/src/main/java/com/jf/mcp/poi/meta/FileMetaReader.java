package com.jf.mcp.poi.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jf.mcp.poi.common.FileAccessValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class FileMetaReader
{
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileAccessValidator fileAccessValidator;

    public FileMetaReader(FileAccessValidator fileAccessValidator)
    {
        this.fileAccessValidator = fileAccessValidator;
    }

    public String getFileMetadata(String filePath) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("fileName", path.getFileName().toString());
        root.put("sizeBytes", attrs.size());
        root.put("isDirectory", attrs.isDirectory());
        root.put("lastModified", Instant.ofEpochMilli(attrs.lastModifiedTime().toMillis()).toString());
        root.put("extension", getExtension(path.getFileName().toString()));
        return root.toString();
    }

    private static String getExtension(String fileName)
    {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1)
        {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
