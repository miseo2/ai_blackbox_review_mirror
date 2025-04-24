package ccrate.s3_test.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;

public class DotenvConfig {

    public static void loadEnv() {
        try {
            // 프로젝트 루트에서 상위 디렉토리로 이동하여 .env 파일 찾기
            File currentDir = new File(".");
            File rootDir = currentDir.getAbsoluteFile().getParentFile().getParentFile().getParentFile();
            File envFile = new File(rootDir, ".env");
    
            if (envFile.exists()) {
                // .env 파일이 존재하면 로드
                Dotenv dotenv = Dotenv.configure()
                        .directory(rootDir.getAbsolutePath())
                        .load();
    
                // 필요한 환경 변수를 시스템 속성으로 설정
                dotenv.entries().forEach(entry -> {
                    if (System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
    
                System.out.println("로컬 환경: .env 파일을 로드했습니다: " + envFile.getAbsolutePath());
            } else {
                System.out.println("CI/CD 환경: .env 파일이 없어 Jenkins Credentials 사용");
            }
        } catch (Exception e) {
            System.out.println("환경 변수 로드 중 오류 발생: " + e.getMessage());
        }
    }
}