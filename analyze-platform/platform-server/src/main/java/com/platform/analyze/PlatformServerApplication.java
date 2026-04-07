package com.platform.analyze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 平台服务端启动类
 */
@SpringBootApplication
@EnableAsync
public class PlatformServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformServerApplication.class, args);
    }
}
