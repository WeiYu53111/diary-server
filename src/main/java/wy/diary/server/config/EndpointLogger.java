package wy.diary.server.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class EndpointLogger implements ApplicationListener<ApplicationReadyEvent> {

    private final RequestMappingHandlerMapping handlerMapping;

    public EndpointLogger(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        handlerMapping.getHandlerMethods().forEach((requestMappingInfo, handlerMethod) -> {
            System.out.println(requestMappingInfo + " -> " + handlerMethod.getMethod().getDeclaringClass().getName() + "#" + handlerMethod.getMethod().getName());
        });
    }
}