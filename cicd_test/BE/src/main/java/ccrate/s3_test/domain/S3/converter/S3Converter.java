package ccrate.s3_test.domain.S3.converter;

import ccrate.s3_test.domain.S3.model.dto.S3Response;
import ccrate.s3_test.domain.S3.model.entity.S3File;
import org.springframework.stereotype.Component;


@Component
public class S3Converter {
    
    public S3Response entityToResponse(S3File s3File) {
        return S3Response.builder()
                .id(s3File.getId())
                .fileName(s3File.getFileName())
                .originalFileName(s3File.getOriginalFileName())
                .url(s3File.getUrl())
                .contentType(s3File.getContentType())
                .fileType(s3File.getFileType())
                .size(s3File.getSize())
                .duration(s3File.getDuration())
                .uploadDate(s3File.getUploadDate())
                .isPresigned(false)
                .build();
    }
    
    public S3Response entityToResponseWithPresignedUrl(S3File s3File, String presignedUrl) {
        S3Response response = entityToResponse(s3File);
        response.setPresignedUrl(presignedUrl);
        response.setPresigned(true);
        return response;
    }
}
