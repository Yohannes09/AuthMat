package com.authmat.application.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "authmat.public-paths")
public record PublicPathsProperties(Map<String,String> publicPaths) {
}
