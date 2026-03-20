package com.jf.mcp.poi.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jf.mcp.poi.common.FileAccessValidator;
import com.jf.mcp.poi.common.TextSearchUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ExcelReader
{
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataFormatter dataFormatter = new DataFormatter();
    private final FileAccessValidator fileAccessValidator;

    public ExcelReader(FileAccessValidator fileAccessValidator)
    {
        this.fileAccessValidator = fileAccessValidator;
    }

    /**
     * 分页读取 Excel 数据，兼容 xls/xlsx。
     *
     * @param startRow 起始行（0-based）
     * @param pageSize 读取行数
     */
    public String readSheetPage(String filePath, String sheetName, int startRow, int pageSize) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        try (InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream))
        {
            Sheet sheet = (sheetName == null || sheetName.isEmpty()) ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null)
            {
                throw new IllegalArgumentException("Sheet not found");
            }

            ArrayNode jsonArray = objectMapper.createArrayNode();
            Row header = sheet.getRow(0);
            if (header == null)
            {
                return "[]";
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : header)
            {
                headers.add(dataFormatter.formatCellValue(cell));
            }

            int lastRow = Math.min(startRow + pageSize, sheet.getLastRowNum() + 1);
            for (int i = Math.max(startRow, 1); i < lastRow; i++)
            {
                Row row = sheet.getRow(i);
                if (row == null)
                {
                    continue;
                }

                ObjectNode node = objectMapper.createObjectNode();
                for (int j = 0; j < headers.size(); j++)
                {
                    node.put(headers.get(j), dataFormatter.formatCellValue(row.getCell(j)));
                }
                jsonArray.add(node);
            }
            return jsonArray.toString();
        }
    }

    public String getExcelMetadata(String filePath) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        try (InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream))
        {
            ObjectNode jsonRoot = objectMapper.createObjectNode();
            ArrayNode sheetsArray = objectMapper.createArrayNode();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                Sheet sheet = workbook.getSheetAt(i);
                ObjectNode sheetNode = objectMapper.createObjectNode();
                sheetNode.put("name", sheet.getSheetName());
                sheetNode.put("totalRows", sheet.getLastRowNum() + 1);

                ArrayNode columnsArray = objectMapper.createArrayNode();
                Row header = sheet.getRow(0);
                if (header != null)
                {
                    for (Cell cell : header)
                    {
                        columnsArray.add(dataFormatter.formatCellValue(cell));
                    }
                }
                sheetNode.set("columns", columnsArray);
                sheetsArray.add(sheetNode);
            }

            jsonRoot.set("sheets", sheetsArray);
            return jsonRoot.toString();
        }
    }

    public int countOccurrences(String filePath, String sheetName, String keyword, boolean useRegex) throws IOException
    {
        Path path = fileAccessValidator.validateReadableFile(filePath);
        try (InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream))
        {
            Sheet sheet = (sheetName == null || sheetName.isEmpty()) ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null)
            {
                throw new IllegalArgumentException("Sheet not found");
            }

            int count = 0;
            for (Row row : sheet)
            {
                for (Cell cell : row)
                {
                    count += TextSearchUtil.countOccurrences(dataFormatter.formatCellValue(cell), keyword, useRegex);
                }
            }
            return count;
        }
    }

    /**
     * 统计指定列中各值的出现次数。
     */
    public List<Map<String, Object>> countColumnValueFrequency(String filePath, String sheetName, String columnName) throws IOException
    {
        Map<String, Integer> frequencyMap = new HashMap<>();
        Path path = fileAccessValidator.validateReadableFile(filePath);
        try (InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream))
        {
            Sheet sheet = (sheetName == null || sheetName.isEmpty()) ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null)
            {
                throw new IllegalArgumentException("Sheet not found");
            }

            Row header = sheet.getRow(0);
            if (header == null)
            {
                return new ArrayList<>();
            }

            int columnIndex = -1;
            for (int i = 0; i < header.getLastCellNum(); i++)
            {
                if (columnName.equals(dataFormatter.formatCellValue(header.getCell(i))))
                {
                    columnIndex = i;
                    break;
                }
            }

            if (columnIndex == -1)
            {
                return new ArrayList<>();
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++)
            {
                Row row = sheet.getRow(i);
                if (row == null)
                {
                    continue;
                }

                String val = dataFormatter.formatCellValue(row.getCell(columnIndex));
                if (val != null && !val.isEmpty())
                {
                    frequencyMap.put(val, frequencyMap.getOrDefault(val, 0) + 1);
                }
            }
        }

        return frequencyMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("value", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .sorted((m1, m2) -> Integer.compare((Integer) m2.get("count"), (Integer) m1.get("count")))
                .collect(Collectors.toList());
    }
}
