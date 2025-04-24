package ccrate.s3_test.domain.S3.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "s3_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class S3File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String fileName;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String url;
    
    @Column(nullable = false)
    private String contentType;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;
    
    @Column(nullable = false)
    private Long size;
    
    // 비디오인 경우에만 사용
    private Integer duration;
    
    @Column(nullable = false)
    private LocalDateTime uploadDate;
    
    @PrePersist
    public void prePersist() {
        this.uploadDate = LocalDateTime.now();
    }
    
    // 이미지 생성자
    public static S3File createImage(String fileName, String originalFileName, String url, String contentType, Long size) {
        S3File file = new S3File();
        file.setFileName(fileName);
        file.setOriginalFileName(originalFileName);
        file.setUrl(url);
        file.setContentType(contentType);
        file.setFileType(FileType.Image);
        file.setSize(size);
        file.setUploadDate(LocalDateTime.now());
        return file;
    }
    
    // 비디오 생성자
    public static S3File createVideo(String fileName, String originalFileName, String url, String contentType, Long size, Integer duration) {
        S3File file = new S3File();
        file.setFileName(fileName);
        file.setOriginalFileName(originalFileName);
        file.setUrl(url);
        file.setContentType(contentType);
        file.setFileType(FileType.Video);
        file.setSize(size);
        file.setDuration(duration);
        file.setUploadDate(LocalDateTime.now());
        return file;
    }
} 