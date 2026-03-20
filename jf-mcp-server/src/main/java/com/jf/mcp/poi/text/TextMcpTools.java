package com.jf.mcp.poi.text;

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
public class TextMcpTools
{
	private final TextFileReader textFileReader;

	public TextMcpTools(TextFileReader textFileReader)
	{
		this.textFileReader = textFileReader;
	}

	@Bean
	public ToolCallbackProvider textTools(TextMcpTools textMcpTools)
	{
		return MethodToolCallbackProvider.builder().toolObjects(textMcpTools).build();
	}

	@Tool(description = "读取文本/Markdown文件内容，支持偏移和最大字符数限制。")
	public String readTextFile(
			@ToolParam(description = "文本文件的本地路径") String filePath,
			@ToolParam(description = "起始偏移（字符数，0-based）") int startOffset,
			@ToolParam(description = "最大读取字符数（<=0 表示不限制）") int maxChars)
	{
		try
		{
			log.info("文本工具-读取文件内容, filePath:{}, startOffset:{}, maxChars:{}", filePath, startOffset, maxChars);
			String content = textFileReader.readText(filePath, startOffset, maxChars);
			log.info("文本工具-读取文件内容完成, contentLength:{}", content.length());
			return content;
		}
		catch (Exception e)
		{
			log.error("文本工具-读取文件内容失败, e:", e);
			return "Error: " + e.getMessage();
		}
	}

	@Tool(description = "统计文本/Markdown文件中关键词出现次数。")
	public int countInTextFile(
			@ToolParam(description = "文本文件的本地路径") String filePath,
			@ToolParam(description = "关键词或正则") String keyword,
			@ToolParam(description = "是否使用正则表达式匹配") boolean useRegex)
	{
		try
		{
			log.info("文本工具-统计关键词频次, filePath:{}, keyword:{}, useRegex:{}", filePath, keyword, useRegex);
			int count = textFileReader.countInText(filePath, keyword, useRegex);
			log.info("文本工具-统计关键词频次完成, count:{}", count);
			return count;
		}
		catch (Exception e)
		{
			log.error("文本工具-统计关键词频次失败, e:", e);
			return -1;
		}
	}

	@Tool(description = "按关键词搜索文本/Markdown文件，返回匹配行及其上下文。")
	public List<String> searchTextFileLines(
			@ToolParam(description = "文本文件的本地路径") String filePath,
			@ToolParam(description = "关键词或正则") String keyword,
			@ToolParam(description = "是否使用正则表达式匹配") boolean useRegex,
			@ToolParam(description = "最大返回行数") int maxLines,
			@ToolParam(description = "返回匹配行前后各几行上下文") int contextLines)
	{
		try
		{
			log.info("文本工具-按关键词搜索行, filePath:{}, keyword:{}, contextLines:{}", filePath, keyword, contextLines);
			List<String> lines = textFileReader.searchTextLines(filePath, keyword, useRegex, maxLines, contextLines);
			log.info("文本工具-按关键词搜索行完成, linesCount:{}", lines.size());
			return lines;
		}
		catch (Exception e)
		{
			log.error("文本工具-按关键词搜索行失败, e:", e);
			return Collections.emptyList();
		}
	}
}
