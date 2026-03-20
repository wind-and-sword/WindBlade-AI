package com.jf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author jf
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class JFApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(JFApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  jf启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "       __  ______   \n" +
                "      / / / ____/   \n" +
                " __  / / / /_       \n" +
                "/ /_/ / / __/       \n" +
                "\\____/ /_/          \n");
    }
}
