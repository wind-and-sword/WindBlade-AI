package com.jf.mcp.poi.word;

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
public class WordMcpTools
{
	private final WordFileReader wordFileReader;

	public WordMcpTools(WordFileReader wordFileReader)
	{
		this.wordFileReader = wordFileReader;
	}

	@Bean
	public ToolCallbackProvider wordTools(WordMcpTools wordMcpTools)
	{
		return MethodToolCallbackProvider.builder().toolObjects(wordMcpTools).build();
	}

	@Tool(description = "读取 Word (.doc/.docx) 文件内容，支持偏移量和最大字符限制，适合读取长文档。")
	public String readWordFile(
			@ToolParam(description = "Word 文件的本地路径") String filePath,
			@ToolParam(description = "起始偏移量（字符数，0-based）") int startOffset,
			@ToolParam(description = "最大读取字符数（<=0 表示不限制）") int maxChars)
	{
		try
		{
			log.info("Word工具-读取文件内容, filePath:{}, startOffset:{}, maxChars:{}", filePath, startOffset, maxChars);
			String content = wordFileReader.readWord(filePath, startOffset, maxChars);
			log.info("Word工具-读取文件内容完成, contentLength:{}", content.length());
			return content;
		}
		catch (Exception e)
		{
			log.error("Word工具-读取文件内容失败, e:", e);
			return "Error: " + e.getMessage();
		}
	}

	@Tool(description = "统计 Word (.doc/.docx) 文件中关键词出现次数。")
	public int countInWordFile(
			@ToolParam(description = "Word 文件的本地路径") String filePath,
			@ToolParam(description = "关键词或正则") String keyword,
			@ToolParam(description = "是否使用正则") boolean useRegex)
	{
		try
		{
			log.info("Word工具-统计关键词频次, filePath:{}, keyword:{}, useRegex:{}", filePath, keyword, useRegex);
			int count = wordFileReader.countInWord(filePath, keyword, useRegex);
			log.info("Word工具-统计关键词频次完成, count:{}", count);
			return count;
		}
		catch (Exception e)
		{
			log.error("Word工具-统计关键词频次失败, e:", e);
			return -1;
		}
	}

	@Tool(description = "按关键词搜索 Word 文件，返回匹配行及其上下文。")
	public List<String> searchWordFileLines(
			@ToolParam(description = "Word 文件的本地路径") String filePath,
			@ToolParam(description = "关键词或正则") String keyword,
			@ToolParam(description = "是否使用正则") boolean useRegex,
			@ToolParam(description = "最大返回匹配项数量") int maxLines,
			@ToolParam(description = "返回匹配行前后各几行上下文（0表示不返回上下文）") int contextLines)
	{
		try
		{
			log.info("Word工具-按关键词搜索行, filePath:{}, keyword:{}, contextLines:{}", filePath, keyword, contextLines);
			List<String> lines = wordFileReader.searchWordLines(filePath, keyword, useRegex, maxLines, contextLines);
			log.info("Word工具-按关键词搜索行完成, linesCount:{}", lines.size());
			return lines;
		}
		catch (Exception e)
		{
			log.error("Word工具-按关键词搜索行失败, e:", e);
			return Collections.emptyList();
		}
	}
}
