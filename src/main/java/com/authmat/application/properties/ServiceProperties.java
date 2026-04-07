package com.authmat.application.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "authmat")
public record ServiceProperties(Map<String,ServiceDefinition> services) {

    public record ServiceDefinition(List<String> scopes){}

    public Set<String> serviceNames(){
        return services.keySet();
    }
}
