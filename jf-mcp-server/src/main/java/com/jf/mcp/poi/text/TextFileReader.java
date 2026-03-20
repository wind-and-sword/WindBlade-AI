package com.jf.mcp.poi.text;

import com.jf.mcp.poi.common.FileAccessProperties;
import com.jf.mcp.poi.common.FileAccessValidator;
import com.jf.mcp.poi.common.TextSearchUtil;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TextFileReader
{
    private final FileAccessValidator fileAccessValidator;
    private final FileAccessProperties fileAccessProperties;

    public TextFileReader(FileAccessValidator fileAccessValidator, FileAccessProperties fileAccessProperties)
    {
        this.fileAccessValidator = fileAccessValidator;
        this.fileAccessProperties = fileAccessProperties;
    }

    public String readText(String filePath, int startOffset, int maxChars) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        String content = readTextWithFallback(path);
        return TextSearchUtil.trimToRange(content, startOffset, maxChars);
    }

    public int countInText(String filePath, String keyword, boolean useRegex) throws IOException
    {
        String text = readText(filePath, 0, -1);
        return TextSearchUtil.countOccurrences(text, keyword, useRegex);
    }

    public List<String> searchTextLines(String filePath, String keyword, boolean useRegex, int maxLines, int contextLines) throws IOException
    {
        String text = readText(filePath, 0, -1);
        return TextSearchUtil.searchLines(text, keyword, useRegex, maxLines, contextLines);
    }

    private String readTextWithFallback(Path path) throws IOException
    {
        try
        {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (MalformedInputException ex)
        {
            return Files.readString(path, fileAccessProperties.resolveFallbackCharset());
        }
    }
}
