package com.authmat.application.token.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsAsyncClient;

@Configuration
public class AwsKmsConfig {
    // NOTE: DefaultCredentialsProvider will automatically pick up AWS_ACCESS_KEY_ID and
    // AWS_SECRET_ACCESS_KEY env variables
//    @Bean
//    public KmsClient kmsClient(@Value("aws.region")String region) {
//        return KmsClient.builder()
//                .region(Region.of(region))
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build();
//    }

    @Bean
    public KmsAsyncClient kmsAsyncClient(@Value("aws.region")String region) {
        return KmsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
