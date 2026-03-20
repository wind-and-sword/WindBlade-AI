package com.jf.mcp.poi.common;

import java.nio.charset.Charset;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mcp.file-access")
public class FileAccessProperties
{
    private String allowedRoot = "D:/jf/uploadPath/chat/mcp";

    private String fallbackCharset = "GB18030";

    public String getAllowedRoot()
    {
        return allowedRoot;
    }

    public void setAllowedRoot(String allowedRoot)
    {
        this.allowedRoot = allowedRoot;
    }

    public String getFallbackCharset()
    {
        return fallbackCharset;
    }

    public void setFallbackCharset(String fallbackCharset)
    {
        this.fallbackCharset = fallbackCharset;
    }

    public Path getAllowedRootPath()
    {
        return Path.of(allowedRoot).toAbsolutePath().normalize();
    }

    public Charset resolveFallbackCharset()
    {
        return Charset.forName(fallbackCharset);
    }
}
