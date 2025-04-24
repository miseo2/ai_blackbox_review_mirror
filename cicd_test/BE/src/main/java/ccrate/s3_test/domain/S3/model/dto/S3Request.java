package ccrate.s3_test.domain.S3.model.dto;

import ccrate.s3_test.domain.S3.model.entity.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class S3Request {
    private String fileName; // 클라이언트의 원본 파일명
    private FileType fileType;
    private Long size; // 파일 크기 (바이트)
    private String contentType; // 파일 타입 (MIME 타입)
}
