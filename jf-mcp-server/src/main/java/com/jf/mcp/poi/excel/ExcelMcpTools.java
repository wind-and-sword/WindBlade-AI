package com.jf.mcp.poi.excel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ExcelMcpTools
{
	private final ExcelReader excelReader;

	public ExcelMcpTools(ExcelReader excelReader) { this.excelReader = excelReader; }

	@Bean
	public ToolCallbackProvider excelTools(ExcelMcpTools excelMcpTools) {
		return MethodToolCallbackProvider.builder().toolObjects(excelMcpTools).build();
	}

	@Tool(description = "分页读取Excel数据。如果文件行数超过100行，请务必使用此方法分页读取，防止超时。")
	public String readExcelPage(
			@ToolParam(description = "文件路径") String filePath,
			@ToolParam(description = "工作表名称") String sheetName,
			@ToolParam(description = "起始行（0-based，含表头）") int startRow,
			@ToolParam(description = "读取行数（建议不超过50行）") int pageSize) {
		try {
			log.info("Excel工具-分页读取内容, filePath:{}, sheetName:{}, startRow:{}, pageSize:{}", filePath, sheetName, startRow, pageSize);
			String result = excelReader.readSheetPage(filePath, sheetName, startRow, pageSize);
			log.info("Excel工具-分页读取内容完成, result:{}", result);
			return result;
		} catch (Exception e) {
			log.error("Excel工具-分页读取内容失败, e:", e);
			return "Error: " + e.getMessage();
		}
	}

	@Tool(description = "获取Excel元数据（Sheet名、总行数、列名）。处理大文件前建议先调用此方法。")
	public String getExcelMetadata(@ToolParam(description = "文件路径") String filePath) {
		try {
			log.info("Excel工具-获取元数据, filePath:{}", filePath);
			String result = excelReader.getExcelMetadata(filePath);
			log.info("Excel工具-获取元数据完成, result:{}", result);
			return result;
		} catch (Exception e) {
			log.error("Excel工具-获取元数据失败, e:", e);
			return "Error: " + e.getMessage();
		}
	}

	@Tool(description = "统计Excel中关键词出现的总次数。此工具在服务器端执行，速度快，适合大文件统计。")
	public int countInExcel(
			@ToolParam(description = "文件路径") String filePath,
			@ToolParam(description = "工作表名称") String sheetName,
			@ToolParam(description = "关键词") String keyword,
			@ToolParam(description = "是否使用正则") boolean useRegex) {
		try {
			log.info("Excel工具-统计关键词频次, filePath:{}, sheetName:{}, keyword:{}, useRegex:{}", filePath, sheetName, keyword, useRegex);
			int count = excelReader.countOccurrences(filePath, sheetName, keyword, useRegex);
			log.info("Excel工具-统计关键词频次完成, keyword:{},count:{}", keyword,count);
			return count;
		} catch (Exception e) {
			log.error("Excel工具-统计关键词频次失败, e:", e);
			return -1;
		}
	}

	@Tool(description = "统计Excel中指定列中每个值的出现次数。返回一个按出现次数降序排列的列表。")
	public List<Map<String, Object>> countColumnValueFrequency(
			@ToolParam(description = "文件路径") String filePath,
			@ToolParam(description = "工作表名称") String sheetName,
			@ToolParam(description = "列名") String columnName) {
		try {
			log.info("Excel工具-统计指定列频次, filePath:{}, sheetName:{}, columnName:{}", filePath, sheetName, columnName);
			List<Map<String, Object>> result = excelReader.countColumnValueFrequency(filePath, sheetName, columnName);
			log.info("Excel工具-统计指定列频次完成, result:{}", result);
			return result;
		} catch (Exception e) {
			log.error("Excel工具-统计指定列频次失败, e:", e);
			return Collections.emptyList();
		}
	}
}
