package com.jf.mcp.poi.pdf;

import com.jf.mcp.poi.common.FileAccessValidator;
import com.jf.mcp.poi.common.TextSearchUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfFileReader
{
    private final FileAccessValidator fileAccessValidator;

    public PdfFileReader(FileAccessValidator fileAccessValidator)
    {
        this.fileAccessValidator = fileAccessValidator;
    }

    public String readPdf(String filePath, int startPage, int endPage, int maxChars) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        try (PDDocument document = PDDocument.load(path.toFile()))
        {
            int totalPages = document.getNumberOfPages();
            int from = startPage > 0 ? startPage : 1;
            int to = endPage > 0 ? Math.min(endPage, totalPages) : totalPages;
            if (from > to)
            {
                throw new IllegalArgumentException("startPage 不能大于 endPage");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(from);
            stripper.setEndPage(to);
            stripper.setSortByPosition(true);
            return TextSearchUtil.trimToRange(stripper.getText(document), 0, maxChars);
        }
    }

    public int countInPdf(String filePath, String keyword, boolean useRegex) throws IOException
    {
        String text = readPdf(filePath, 1, 0, -1);
        return TextSearchUtil.countOccurrences(text, keyword, useRegex);
    }

    public List<String> searchPdfLines(String filePath, String keyword, boolean useRegex, int maxLines, int contextLines) throws IOException
    {
        String text = readPdf(filePath, 1, 0, -1);
        return TextSearchUtil.searchLines(text, keyword, useRegex, maxLines, contextLines);
    }
}
