package com.authmat.application.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatVirtualThreads() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.getProtocolHandler()
                    .setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        });
    }
}
