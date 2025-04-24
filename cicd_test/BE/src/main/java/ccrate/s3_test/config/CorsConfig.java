package ccrate.s3_test.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;

import jakarta.annotation.PostConstruct;

@Configuration
public class CorsConfig {

    @Autowired
    private S3Client s3Client;
    
    @Value("${aws.bucketName}")
    private String bucketName;
    
    @PostConstruct
    public void setupCors() {
        try {
            // CORS 규칙 생성
            CORSRule corsRule = CORSRule.builder()
                    .allowedOrigins("*") // 모든 출처 허용 (프로덕션에서는 특정 도메인만 허용하는 것이 좋음)
                    .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
                    .allowedHeaders("*")
                    .maxAgeSeconds(3000)
                    .build();

            // CORS 구성에 규칙 추가
            CORSConfiguration corsConfiguration = CORSConfiguration.builder()
                    .corsRules(corsRule)
                    .build();

            // S3 버킷에 CORS 설정 적용
            PutBucketCorsRequest putBucketCorsRequest = PutBucketCorsRequest.builder()
                    .bucket(bucketName)
                    .corsConfiguration(corsConfiguration)
                    .build();

            s3Client.putBucketCors(putBucketCorsRequest);
            System.out.println("S3 버킷 CORS 설정이 완료되었습니다.");
        } catch (Exception e) {
            System.err.println("S3 버킷 CORS 설정 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 