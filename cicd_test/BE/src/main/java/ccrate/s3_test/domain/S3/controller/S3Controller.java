package ccrate.s3_test.domain.S3.controller;

import ccrate.s3_test.domain.S3.model.dto.S3Request;
import ccrate.s3_test.domain.S3.model.dto.S3Response;
import ccrate.s3_test.domain.S3.model.entity.FileType;
import ccrate.s3_test.domain.S3.service.S3Service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {
    
    private final S3Service s3Service;

    /**
     * Presigned URL 발급 요청
     * 클라이언트가 파일 업로드 의사를 알리면 업로드할 수 있는 URL 발급
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<S3Response> getPresignedUrl(@RequestBody S3Request request) {
        FileType fileType = request.getFileType();
        S3Response response = s3Service.generatePresignedUrl(request.getFileName(), fileType);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * 업로드 완료 확인
     * 클라이언트가 Presigned URL로 업로드 완료 후 호출
     */
    @PostMapping("/confirm-upload")
    public ResponseEntity<Map<String, String>> confirmUpload(@RequestBody Map<String, Object> request) {
        String fileName = (String) request.get("fileName");
        String originalFileName = (String) request.get("originalFileName");
        String fileTypeStr = (String) request.get("fileType");
        Long size = Long.valueOf(request.get("size").toString());
        
        FileType fileType = convertToFileType(fileTypeStr);
        s3Service.confirmUpload(fileName, originalFileName, fileType, size);
        
        return ResponseEntity.ok(Map.of("message", "파일 업로드가 확인되었습니다."));
    }
    
    /**
     * 기존 파일에 대한 Presigned URL 발급
     * 다운로드나 조회를 위한 URL 발급
     */
    @GetMapping("/presigned-url/{fileName}")
    public ResponseEntity<?> getPresignedUrlForFile(@PathVariable String fileName) {
        return s3Service.generatePresignedUrlForFile(fileName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 파일 삭제
     */
    @DeleteMapping("/files/{fileType}/{fileName}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable String fileType, 
            @PathVariable String fileName) {
        
        FileType type = convertToFileType(fileType);
        s3Service.deleteFile(fileName, type);
        
        return ResponseEntity.ok(Map.of("message", "파일이 삭제되었습니다."));
    }
    
    /**
     * 파일 URL 조회
     */
    @GetMapping("/files/{fileType}/{fileName}")
    public ResponseEntity<Map<String, String>> getFileUrl(
            @PathVariable String fileType, 
            @PathVariable String fileName) {
        
        FileType type = convertToFileType(fileType);
        String url = s3Service.getFileUrl(fileName, type);
        
        return ResponseEntity.ok(Map.of("url", url));
    }
    
    /**
     * 단일 파일 조회 (presigned URL 포함)
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<?> getFile(@PathVariable String fileName) {
        return s3Service.getFile(fileName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 모든 파일 목록 조회 (presigned URL 포함)
     */
    @GetMapping("/files")
    public ResponseEntity<?> getAllFiles() {
        return s3Service.getAllFiles()
                .map(files -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("count", files.size());
                    response.put("files", files);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.noContent().build());
    }
    
    /**
     * 파일 타입별 목록 조회 (presigned URL 포함)
     */
    @GetMapping("/files/{fileType}")
    public ResponseEntity<?> getFilesByType(@PathVariable String fileType) {
        FileType type = convertToFileType(fileType);
        return s3Service.getFilesByType(type)
                .map(files -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("count", files.size());
                    response.put("fileType", type.toString());
                    response.put("files", files);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.noContent().build());
    }
    
    /**
     * 문자열을 FileType Enum으로 변환
     */
    private FileType convertToFileType(String fileType) {
        return "Image".equalsIgnoreCase(fileType) ? FileType.Image : FileType.Video;
    }
}
