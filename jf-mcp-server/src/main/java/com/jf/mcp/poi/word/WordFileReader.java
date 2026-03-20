package com.jf.mcp.poi.word;

import com.jf.mcp.poi.common.FileAccessValidator;
import com.jf.mcp.poi.common.TextSearchUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class WordFileReader
{
    private final FileAccessValidator fileAccessValidator;

    public WordFileReader(FileAccessValidator fileAccessValidator)
    {
        this.fileAccessValidator = fileAccessValidator;
    }

    public String readWord(String filePath, int startOffset, int maxChars) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        String normalizedFileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        String text;

        try (FileInputStream fis = new FileInputStream(path.toFile()))
        {
            if (normalizedFileName.endsWith(".docx"))
            {
                try (XWPFDocument document = new XWPFDocument(fis);
                        XWPFWordExtractor extractor = new XWPFWordExtractor(document))
                {
                    text = extractor.getText();
                }
            }
            else if (normalizedFileName.endsWith(".doc"))
            {
                try (WordExtractor extractor = new WordExtractor(fis))
                {
                    text = extractor.getText();
                }
            }
            else
            {
                throw new IllegalArgumentException("不支持的文件格式: " + path.getFileName());
            }
        }

        return TextSearchUtil.trimToRange(text, startOffset, maxChars);
    }

    public int countInWord(String filePath, String keyword, boolean useRegex) throws IOException
    {
        String text = readWord(filePath, 0, -1);
        return TextSearchUtil.countOccurrences(text, keyword, useRegex);
    }

    public List<String> searchWordLines(String filePath, String keyword, boolean useRegex, int maxLines, int contextLines) throws IOException
    {
        String text = readWord(filePath, 0, -1);
        return TextSearchUtil.searchLines(text, keyword, useRegex, maxLines, contextLines);
    }
}
