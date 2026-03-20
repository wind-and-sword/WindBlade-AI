package com.jf.web.controller.chat.mcp;

import com.jf.common.config.JFConfig;
import com.jf.common.constant.Constants;
import com.jf.common.core.domain.AjaxResult;
import com.jf.common.utils.file.FileUploadUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

/**
 * MCP 文件分析控制器。
 */
@RestController
@RequestMapping("/api/mcp")
public class McpChatClientController
{
    private static final Logger log = LoggerFactory.getLogger(McpChatClientController.class);
    private static final String[] ALLOWED_EXTENSIONS = { "xls", "xlsx", "txt", "md", "doc", "docx", "pdf" };
    private static final String UPLOAD_SUB_DIR = "/chat/mcp";
    private static final Map<String, String> FILE_STORAGE_MAP = new ConcurrentHashMap<>();

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public McpChatClientController(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider)
    {
        this.chatClient = builder.build();
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @PostMapping("/upload")
    public AjaxResult uploadFile(@RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return AjaxResult.error("请选择文件");
        }

        try
        {
            String uploadPath = JFConfig.getProfile() + UPLOAD_SUB_DIR;
            String fileName = FileUploadUtils.upload(uploadPath, file, ALLOWED_EXTENSIONS, true);
            String relativePath = fileName.replace(Constants.RESOURCE_PREFIX + "/", "");
            String absolutePath = FileUploadUtils.getAbsoluteFile(JFConfig.getProfile(), relativePath).getAbsolutePath();
            String fileId = UUID.randomUUID().toString().replace("-", "");
            FILE_STORAGE_MAP.put(fileId, absolutePath);

            log.info("MCP 文件上传完成, fileId:{}, originalFileName:{}", fileId, file.getOriginalFilename());
            return AjaxResult.success("上传成功").put("fileId", fileId).put("fileName", file.getOriginalFilename());
        }
        catch (Exception e)
        {
            log.error("MCP 文件上传失败", e);
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 流式对话接口。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestParam("fileId") String fileId, @RequestParam("text") String text)
    {
        String absolutePath = FILE_STORAGE_MAP.get(fileId);
        if (absolutePath == null)
        {
            return Flux.just(ServerSentEvent.<String>builder().data("Error: 文件不存在或分析会话已过期").build());
        }

        log.info("MCP 对话分析开始, fileId:{}", fileId);

        String systemPrompt = """
                # Role: 全能文件分析专家 (Multi-Format Document Analyst)

                ## 核心定位
                你是一个拥有深厚文档解析经验的资深分析师。你能精准理解用户意图，熟练运用各类 MCP 工具（Word、Excel、PDF、Text）对授权文件进行深度挖掘。

                ## 任务逻辑
                1. 首次接触文件时，优先获取元数据，确认文件规模、编码、工作表名称等基础信息。
                2. 判断用户需要的是全局摘要、细节提取还是逻辑对比，再决定分析深度。
                3. 针对长文档采用多点采样或分段读取策略，单次读取控制在 5000 字符以内。
                4. 如果关键词搜索未命中，应尝试模糊搜索或按段落顺序扫描，避免遗漏关键内容。

                ## 工具调用准则
                - 调用工具时必须直接、完整地使用给定的文件路径参数。
                - 只允许分析当前提供的授权文件，不得自行构造、替换或扩展为其他本地路径。
                - 根据文件后缀名精准选择对应工具，优先使用 count 和 search 定位，再用 read 提取细节。

                ## 回复规范
                - 不要在最终回答中暴露工具名称、调用过程或绝对路径。
                - 如需提及文件，仅展示文件名。
                - 始终使用专业、自然的中文回答。

                ## 异常容错
                若工具报错或未获取到预期内容，请说明可能的限制，并给出替代分析建议。
                """;

        String userPrompt = String.format("""
                [文件路径]: %s
                [用户问题]: %s

                请严格基于上述文件完成分析。
                """, absolutePath, text);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder().data(content).build())
                .onErrorResume(e -> {
                    log.error("MCP 对话流异常, fileId:{}", fileId, e);
                    return Flux.just(ServerSentEvent.<String>builder().data("Error: " + e.getMessage()).build());
                });
    }
}
