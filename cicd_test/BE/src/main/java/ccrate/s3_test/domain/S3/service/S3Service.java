package ccrate.s3_test.domain.S3.service;

import ccrate.s3_test.domain.S3.model.dto.S3Response;
import ccrate.s3_test.domain.S3.model.entity.FileType;

import java.util.List;
import java.util.Optional;

public interface S3Service {
    // Presigned URL 생성 (업로드용)
    S3Response generatePresignedUrl(String originalFileName, FileType fileType);
    
    // 단일 파일에 대한 presigned URL 생성 (조회/다운로드용)
    Optional<S3Response> generatePresignedUrlForFile(String fileName);
    
    // 파일 업로드 완료 처리
    void confirmUpload(String fileName, String originalFileName, FileType fileType, Long size);
    
    // 파일 삭제
    void deleteFile(String fileName, FileType fileType);
    
    // 파일 URL 가져오기
    String getFileUrl(String fileName, FileType fileType);
    
    // 단일 파일 조회 (presigned URL 포함)
    Optional<S3Response> getFile(String fileName);
    
    // 모든 파일 목록 조회 (presigned URL 포함)
    Optional<List<S3Response>> getAllFiles();
    
    // 특정 타입의 파일 목록 조회 (presigned URL 포함)
    Optional<List<S3Response>> getFilesByType(FileType fileType);
}
