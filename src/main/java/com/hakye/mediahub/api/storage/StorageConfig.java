package com.hakye.mediahub.api.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private static StaticCredentialsProvider credentials(StorageProperties p) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(p.accessKeyId(), p.secretAccessKey()));
    }

    private static URI endpoint(StorageProperties p) {
        return URI.create("https://" + p.accountId() + ".r2.cloudflarestorage.com");
    }

    private static S3Configuration pathStyle() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    @Bean
    public S3Client s3Client(StorageProperties p) {
        return S3Client.builder()
                .endpointOverride(endpoint(p))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(p))
                .serviceConfiguration(pathStyle())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties p) {
        return S3Presigner.builder()
                .endpointOverride(endpoint(p))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(p))
                .serviceConfiguration(pathStyle())
                .build();
    }
}
