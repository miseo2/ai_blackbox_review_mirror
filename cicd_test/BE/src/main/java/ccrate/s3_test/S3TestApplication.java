package ccrate.s3_test;


import ccrate.s3_test.config.DotenvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class S3TestApplication {

	public static void main(String[] args) {
		// 애플리케이션 시작 전에 환경 변수 로드
		DotenvConfig.loadEnv();
		SpringApplication.run(S3TestApplication.class, args);
	}

}
