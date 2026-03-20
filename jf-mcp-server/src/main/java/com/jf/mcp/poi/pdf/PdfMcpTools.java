package com.jf.mcp.poi.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class PdfMcpTools
{
	private final PdfFileReader pdfFileReader;

	public PdfMcpTools(PdfFileReader pdfFileReader)
	{
		this.pdfFileReader = pdfFileReader;
	}

	@Bean
	public ToolCallbackProvider pdfTools(PdfMcpTools pdfMcpTools)
	{
		return MethodToolCallbackProvider.builder().toolObjects(pdfMcpTools).build();
	}

	@Tool(description = "读取PDF文件内容，返回纯文本")
	public String readPdfFile(
			@ToolParam(description = "PDF 文件的本地路径") String filePath,
			@ToolParam(description = "起始页（1-based）") int startPage,
			@ToolParam(description = "结束页（1-based，0 表示到最后）") int endPage,
			@ToolParam(description = "最大读取字符数（<=0 表示不限制）") int maxChars)
	{
		try
		{
			log.info("PDF工具-读取文件内容, filePath:{}, startPage:{}, endPage:{}, maxChars:{}",
					filePath, startPage, endPage, maxChars);
			String content = pdfFileReader.readPdf(filePath, startPage, endPage, maxChars);
			log.info("PDF工具-读取文件内容完成, contentLength:{}", content.length());
			return content;
		}
		catch (Exception e)
		{
			log.error("PDF工具-读取文件内容失败, e:", e);
			return "Error: " + e.getMessage();
		}
	}

	@Tool(description = "统计PDF文件中关键词出现次数。")
	public int countInPdfFile(
			@ToolParam(description = "PDF 文件的本地路径") String filePath,
			@ToolParam(description = "关键词或正则") String keyword,
			@ToolParam(description = "是否使用正则表达式") boolean useRegex)
	{
		try
		{
			log.info("PDF工具-统计关键词频次, filePath:{}, keyword:{}, useRegex:{}", filePath, keyword, useRegex);
			int count = pdfFileReader.countInPdf(filePath, keyword, useRegex);
			log.info("PDF工具-统计关键词频次完成, count:{}", count);
			return count;
		}
		catch (Exception e)
		{
			log.error("PDF工具-统计关键词频次失败, e:", e);
			return -1;
		}
	}

	@Tool(description = "按关键词搜索PDF文件，返回匹配行及其上下文。")
	public List<String> searchPdfFileLines(
			@ToolParam(description = "PDF 文件的本地路径") String filePath,
			@ToolParam(description = "关键词或正则") String keyword,
			@ToolParam(description = "是否使用正则表达式") boolean useRegex,
			@ToolParam(description = "最大返回匹配项数量") int maxLines,
			@ToolParam(description = "返回匹配行前后各几行上下文（0表示不返回上下文）") int contextLines)
	{
		try
		{
			log.info("PDF工具-按关键词搜索行, filePath:{}, keyword:{}, contextLines:{}", filePath, keyword, contextLines);
			List<String> lines = pdfFileReader.searchPdfLines(filePath, keyword, useRegex, maxLines, contextLines);
			log.info("PDF工具-按关键词搜索行完成, linesCount:{}", lines.size());
			return lines;
		}
		catch (Exception e)
		{
			log.error("PDF工具-按关键词搜索行失败, e:", e);
			return Collections.emptyList();
		}
	}
}
