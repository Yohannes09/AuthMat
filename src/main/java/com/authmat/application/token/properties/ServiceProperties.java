package com.authmat.application.token.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
@ConfigurationProperties(prefix = "authmat")
public record ServiceProperties(Map<String,ServiceDefinition> services) {

    public record ServiceDefinition(List<String> scopes){}
}
