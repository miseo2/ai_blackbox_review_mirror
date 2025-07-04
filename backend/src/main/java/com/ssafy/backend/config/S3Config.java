package com.ssafy.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Getter
public class S3Config {

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    private S3Client s3Client;
    private S3Presigner s3Presigner; //Presigned URL위한 코드 추가

    @PostConstruct //서버 시작할 때 딱 1번만 S3Client 만들어줌
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider provider = StaticCredentialsProvider.create(credentials);

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build(); //원래는 이거만 있었는데 Presigned URL위해 위아래 코드 추가됨

        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build();
    }
    @Bean
    public S3Presigner s3Presigner() {
        return this.s3Presigner;
    }

    @Bean
    public S3Client s3Client() {
        return this.s3Client;
    }

}