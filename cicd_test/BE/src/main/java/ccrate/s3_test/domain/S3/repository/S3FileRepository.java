package ccrate.s3_test.domain.S3.repository;

import ccrate.s3_test.domain.S3.model.entity.S3File;
import ccrate.s3_test.domain.S3.model.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface S3FileRepository extends JpaRepository<S3File, Long> {
    Optional<S3File> findByFileName(String fileName);
    void deleteByFileName(String fileName);
    
    // 모든 파일 조회 (기본 findAll() 외에 추가 옵션)
    List<S3File> findAllByOrderByUploadDateDesc(); // 최신 업로드 순
    
    // 파일 타입별 조회
    List<S3File> findByFileType(FileType fileType);
    List<S3File> findByFileTypeOrderByUploadDateDesc(FileType fileType);
} 