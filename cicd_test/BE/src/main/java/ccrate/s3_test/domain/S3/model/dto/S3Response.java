package ccrate.s3_test.domain.S3.model.dto;

import ccrate.s3_test.domain.S3.model.entity.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3Response {
    // 기본 파일 정보
    private Long id;
    private String fileName;
    private String originalFileName;
    private String url;
    private String contentType;
    private FileType fileType;
    private Long size;
    private Integer duration;
    private LocalDateTime uploadDate;
    
    // Presigned URL 관련
    private String presignedUrl;
    private boolean isPresigned;
}
