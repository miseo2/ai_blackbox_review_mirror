package ccrate.s3_test.domain.S3.service;

import ccrate.s3_test.domain.S3.converter.S3Converter;
import ccrate.s3_test.domain.S3.model.dto.S3Response;
import ccrate.s3_test.domain.S3.model.entity.FileType;
import ccrate.s3_test.domain.S3.model.entity.S3File;
import ccrate.s3_test.domain.S3.repository.S3FileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3FileRepository s3FileRepository;
    private final S3Converter s3Converter;
    
    @Value("${aws.bucketName}")
    private String bucketName;
    
    @Value("${aws.s3.presigned-url.expiration}")
    private Long presignedUrlExpiration;

    @Override
    public S3Response generatePresignedUrl(String originalFileName, FileType fileType) {
        // 파일명 생성 (UUID + 확장자)
        String extension = getExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + extension;
        
        // 저장 경로 설정
        String prefix = fileType == FileType.Image ? "images/" : "videos/";
        String objectKey = prefix + fileName;
        
        // Presigned URL 생성 (PUT 요청용)
        Map<String, String> metadata = new HashMap<>();
        metadata.put("originalFileName", originalFileName);
        
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .metadata(metadata)
                .build();
                
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpiration)) // 설정 파일의 만료 시간 사용
                .putObjectRequest(objectRequest)
                .build();
                
        URL presignedUrl = s3Presigner.presignPutObject(presignRequest).url();
        
        // 응답 생성
        S3Response response = new S3Response();
        response.setFileName(fileName);
        response.setOriginalFileName(originalFileName);
        response.setFileType(fileType);
        response.setPresignedUrl(presignedUrl.toString());
        response.setPresigned(true);
        
        return response;
    }
    
    @Override
    public Optional<S3Response> generatePresignedUrlForFile(String fileName) {
        return s3FileRepository.findByFileName(fileName)
                .map(s3File -> {
                    try {
                        // 저장 경로 설정
                        String prefix = s3File.getFileType() == FileType.Image ? "images/" : "videos/";
                        String objectKey = prefix + fileName;
                        
                        // Presigned URL 생성 (GET 요청용)
                        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(objectKey)
                                .build();
                                
                        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                                .signatureDuration(Duration.ofMinutes(10)) // 10분 유효
                                .getObjectRequest(getObjectRequest)
                                .build();
                                
                        URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();
                        
                        // 응답 생성
                        return s3Converter.entityToResponseWithPresignedUrl(s3File, presignedUrl.toString());
                    } catch (Exception e) {
                        log.error("Failed to generate presigned URL for file {}: {}", fileName, e.getMessage());
                        return s3Converter.entityToResponse(s3File);
                    }
                });
    }
    
    @Override
    public void confirmUpload(String fileName, String originalFileName, FileType fileType, Long size) {
        // 파일이 실제로 업로드되었는지 확인
        String prefix = fileType == FileType.Image ? "images/" : "videos/";
        String objectKey = prefix + fileName;
        
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        
        try {
            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            
            // 파일 URL 생성
            String fileUrl = getFileUrl(fileName, fileType);
            
            // DB에 메타데이터 저장
            S3File s3File;
            if (fileType == FileType.Image) {
                s3File = S3File.createImage(fileName, originalFileName, fileUrl, headObjectResponse.contentType(), size);
            } else {
                s3File = S3File.createVideo(fileName, originalFileName, fileUrl, headObjectResponse.contentType(), size, null);
            }
            
            // 저장 - JpaRepository의 save 메소드 사용
            s3FileRepository.save(s3File);
            
            // 저장 확인 로그 추가
            log.info("파일 업로드 확인 성공: {}, 타입: {}, 크기: {}", fileName, fileType, size);
        } catch (Exception e) {
            log.error("File upload confirmation failed: {}", e.getMessage());
            throw new RuntimeException("File upload confirmation failed", e);
        }
    }
    
    @Override
    @Transactional
    public void deleteFile(String fileName, FileType fileType) {
        String prefix = fileType == FileType.Image ? "images/" : "videos/";
        String objectKey = prefix + fileName;
        
        try {
            // S3에서 파일 삭제
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3에서 파일 삭제 성공: {}", fileName);
            
            // DB에서 메타데이터 삭제
            s3FileRepository.deleteByFileName(fileName);
            log.info("데이터베이스에서 파일 메타데이터 삭제 성공: {}", fileName);
        } catch (S3Exception s3e) {
            log.error("S3에서 파일 삭제 실패: {}, 오류: {}", fileName, s3e.getMessage());
            throw new RuntimeException("S3에서 파일 삭제 실패", s3e);
        } catch (Exception e) {
            log.error("파일 삭제 처리 중 오류 발생: {}, 오류: {}", fileName, e.getMessage());
            throw new RuntimeException("파일 삭제 처리 중 오류 발생", e);
        }
    }

    @Override
    public String getFileUrl(String fileName, FileType fileType) {
        String prefix = fileType == FileType.Image ? "images/" : "videos/";
        String objectKey = prefix + fileName;
        
        // S3 버킷의 URL 형식으로 리턴
        return "https://" + bucketName + ".s3.amazonaws.com/" + objectKey;
    }
    
    @Override
    public Optional<S3Response> getFile(String fileName) {
        try {
            // 파일명으로 조회
            return generatePresignedUrlForFile(fileName);
        } catch (Exception e) {
            log.error("Failed to get file by name {}: {}", fileName, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<List<S3Response>> getAllFiles() {
        try {
            // 최신 업로드 순으로 모든 파일 조회
            List<S3File> files = s3FileRepository.findAllByOrderByUploadDateDesc();
            if (files.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(createPresignedUrlsForFiles(files));
        } catch (Exception e) {
            log.error("Failed to get all files: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<List<S3Response>> getFilesByType(FileType fileType) {
        try {
            // 특정 타입의 파일만 최신 업로드 순으로 조회
            List<S3File> files = s3FileRepository.findByFileTypeOrderByUploadDateDesc(fileType);
            if (files.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(createPresignedUrlsForFiles(files));
        } catch (Exception e) {
            log.error("Failed to get files by type {}: {}", fileType, e.getMessage());
            return Optional.empty();
        }
    }
    
    // 파일 목록에 대한 presigned URL 생성
    private List<S3Response> createPresignedUrlsForFiles(List<S3File> files) {
        List<S3Response> responses = new ArrayList<>();
        
        for (S3File file : files) {
            try {
                // 저장 경로 설정
                String prefix = file.getFileType() == FileType.Image ? "images/" : "videos/";
                String objectKey = prefix + file.getFileName();
                
                // Presigned URL 생성 (GET 요청용)
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build();
                        
                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10)) // 10분 유효
                        .getObjectRequest(getObjectRequest)
                        .build();
                        
                URL presignedUrl = s3Presigner.presignGetObject(presignRequest).url();
                
                // 응답 생성
                responses.add(s3Converter.entityToResponseWithPresignedUrl(file, presignedUrl.toString()));
            } catch (Exception e) {
                log.warn("Failed to generate presigned URL for file {}: {}", file.getFileName(), e.getMessage());
                responses.add(s3Converter.entityToResponse(file));
            }
        }
        
        return responses;
    }
    
    private String getExtension(String fileName) {
        return fileName != null && fileName.contains(".") 
                ? fileName.substring(fileName.lastIndexOf(".")) 
                : "";
    }
}
