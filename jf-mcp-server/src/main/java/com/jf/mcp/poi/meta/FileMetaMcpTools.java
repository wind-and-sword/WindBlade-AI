package com.jf.mcp.poi.meta;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileMetaMcpTools
{
	private final FileMetaReader fileMetaReader;

	public FileMetaMcpTools(FileMetaReader fileMetaReader)
	{
		this.fileMetaReader = fileMetaReader;
	}

	@Bean
	public ToolCallbackProvider fileMetaTools(FileMetaMcpTools fileMetaMcpTools)
	{
		return MethodToolCallbackProvider.builder().toolObjects(fileMetaMcpTools).build();
	}

	@Tool(description = "读取文件元数据（文件名、大小、扩展名、更新时间等）")
	public String getFileMetadata(
			@ToolParam(description = "文件的本地路径（字符串，必填）") String filePath)
	{
		try
		{
			log.info("元数据工具-读取文件元数据, filePath:{}", filePath);
			String metadata = fileMetaReader.getFileMetadata(filePath);
			log.info("元数据工具-读取文件元数据完成, metadata:{}", metadata);
			return metadata;
		}
		catch (Exception e)
		{
			log.error("元数据工具-读取文件元数据失败, e:", e);
			return "Error: " + e.getMessage();
		}
	}
}
