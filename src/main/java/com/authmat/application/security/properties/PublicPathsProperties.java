package com.authmat.application.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "authmat")
public class PublicPathsProperties{
    private Map<String,String> publicPaths = new HashMap<String,String>();

    public Map<String, String> getPublicPaths(){
        return publicPaths;
    }

    public String[] getPublicPathsArr() {
        return publicPaths.values().toArray(String[]::new);
    }
}
