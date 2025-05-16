package wy.diary.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import wy.diary.server.util.TokenInterceptor;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}

    @Bean
    public WebMvcConfigurer webMvcConfigurer(TokenInterceptor tokenInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(tokenInterceptor)
                        .addPathPatterns("/api/**") // 应用到所有以 /api 开头的请求
                        .excludePathPatterns("/wx/login","/api/images/view/**"); // 排除登录接口
            }
        };
    }
}
