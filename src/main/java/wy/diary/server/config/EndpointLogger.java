package wy.diary.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class EndpointLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EndpointLogger.class);
    
    private final RequestMappingHandlerMapping handlerMapping;

    public EndpointLogger(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("应用程序已启动，打印所有端点映射:");
        handlerMapping.getHandlerMethods().forEach((requestMappingInfo, handlerMethod) -> {
            logger.info("{} -> {}#{}", 
                    requestMappingInfo, 
                    handlerMethod.getMethod().getDeclaringClass().getName(), 
                    handlerMethod.getMethod().getName());
        });
    }
}