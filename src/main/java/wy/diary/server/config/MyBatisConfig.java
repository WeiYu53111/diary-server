package wy.diary.server.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置类
 */
@Configuration
@MapperScan("wy.diary.server.mapper")
public class MyBatisConfig {
    // MyBatis配置
} 