package com.authmat.application.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "authmat.public-paths")
public record PublicPathsProperties(Map<String,String> publicPaths) {
}
