package com.project.service;

import com.project.config.MinIOConfig;
import com.project.entity.mysql.Knowledge;
import com.project.repository.mysql.KnowledgeRepository;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    private static final String KNOWLEDGE_BUCKET = "emergency-knowledge";

    private final KnowledgeRepository knowledgeRepository;
    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;
    private final VectorizeService vectorizeService;

    public KnowledgeService(KnowledgeRepository knowledgeRepository,
                           MinioClient minioClient,
                           MinIOConfig minIOConfig,
                           VectorizeService vectorizeService) {
        this.knowledgeRepository = knowledgeRepository;
        this.minioClient = minioClient;
        this.minIOConfig = minIOConfig;
        this.vectorizeService = vectorizeService;
    }

    @Transactional("mysqlTransactionManager")
    public Knowledge uploadFile(MultipartFile file, String description, Long uploaderId, String uploaderName) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("文件大小不能超过50MB");
            }

            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            if (!".pdf".equals(extension)) {
                throw new IllegalArgumentException("仅支持上传PDF文件");
            }

            String fileId = UUID.randomUUID().toString();
            String dateDir = LocalDateTime.now().format(DATE_FORMATTER);
            String newFilename = fileId.substring(0, 8) + extension;
            String objectKey = "knowledge/" + dateDir + "/" + newFilename;

            ensureBucketExists();

            String contentType = file.getContentType();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(KNOWLEDGE_BUCKET)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );

            Knowledge knowledge = new Knowledge();
            knowledge.setFileId(fileId);
            knowledge.setFileName(originalFilename);
            knowledge.setFileSize(file.getSize());
            knowledge.setFileType(extension.replace(".", ""));
            knowledge.setObjectKey(objectKey);
            knowledge.setBucket(KNOWLEDGE_BUCKET);
            knowledge.setDescription(description);
            knowledge.setUploaderId(uploaderId);
            knowledge.setUploaderName(uploaderName);

            knowledgeRepository.save(knowledge);

            logger.info("Knowledge file uploaded: {} -> {}", originalFilename, objectKey);

            vectorizeService.triggerVectorizeAsync(knowledge.getFileId());

            return knowledge;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to upload knowledge file", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Transactional("mysqlTransactionManager")
    public List<Knowledge> listFiles() {
        return knowledgeRepository.findAll();
    }

    @Transactional("mysqlTransactionManager")
    public void deleteFile(String fileId) {
        Knowledge knowledge = knowledgeRepository.findByFileId(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在，fileId: " + fileId));

        if (Knowledge.STATUS_PROCESSING.equals(knowledge.getVectorizeStatus())) {
            throw new IllegalArgumentException("文件正在处理中，请稍后删除");
        }

        vectorizeService.deleteVectors(knowledge.getFileName());

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(knowledge.getBucket())
                            .object(knowledge.getObjectKey())
                            .build()
            );
            logger.info("Deleted file from MinIO: {}", knowledge.getObjectKey());
        } catch (Exception e) {
            logger.error("Failed to delete file from MinIO: {}", knowledge.getObjectKey(), e);
        }

        knowledgeRepository.deleteByFileId(fileId);
        logger.info("Deleted knowledge record: {}", fileId);
    }

    public GetObjectResponse downloadFile(String fileId) {
        Knowledge knowledge = knowledgeRepository.findByFileId(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在，fileId: " + fileId));

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(knowledge.getBucket())
                            .object(knowledge.getObjectKey())
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to download file from MinIO: {}", knowledge.getObjectKey(), e);
            throw new RuntimeException("文件下载失败", e);
        }
    }

    private void ensureBucketExists() {
        try {
            if (!minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket(KNOWLEDGE_BUCKET)
                            .build())) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder()
                                .bucket(KNOWLEDGE_BUCKET)
                                .build());
                logger.info("Created MinIO bucket: {}", KNOWLEDGE_BUCKET);
            }
        } catch (Exception e) {
            logger.error("Failed to check/create bucket: {}", KNOWLEDGE_BUCKET, e);
        }
    }
}