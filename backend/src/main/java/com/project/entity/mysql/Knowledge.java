package com.project.entity.mysql;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_files")
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", unique = true, nullable = false, length = 50)
    private String fileId;

    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "bucket", nullable = false, length = 100)
    private String bucket;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "uploader_name", length = 100)
    private String uploaderName;

    @Column(name = "vectorize_status", length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String vectorizeStatus;

    @Column(name = "vectorize_fail_reason", length = 500)
    private String vectorizeFailReason;

    @Column(name = "vectorize_started_at")
    private LocalDateTime vectorizeStartedAt;

    @Column(name = "vectorize_completed_at")
    private LocalDateTime vectorizeCompletedAt;

    @Column(name = "vectorize_retry_count", columnDefinition = "int default 0")
    private Integer vectorizeRetryCount;

    @Column(name = "chunk_count", columnDefinition = "int default 0")
    private Integer chunkCount;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Knowledge() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (vectorizeStatus == null) {
            vectorizeStatus = STATUS_PENDING;
        }
        if (vectorizeRetryCount == null) {
            vectorizeRetryCount = 0;
        }
        if (chunkCount == null) {
            chunkCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getVectorizeStatus() { return vectorizeStatus; }
    public void setVectorizeStatus(String vectorizeStatus) { this.vectorizeStatus = vectorizeStatus; }

    public String getVectorizeFailReason() { return vectorizeFailReason; }
    public void setVectorizeFailReason(String vectorizeFailReason) { this.vectorizeFailReason = vectorizeFailReason; }

    public LocalDateTime getVectorizeStartedAt() { return vectorizeStartedAt; }
    public void setVectorizeStartedAt(LocalDateTime vectorizeStartedAt) { this.vectorizeStartedAt = vectorizeStartedAt; }

    public LocalDateTime getVectorizeCompletedAt() { return vectorizeCompletedAt; }
    public void setVectorizeCompletedAt(LocalDateTime vectorizeCompletedAt) { this.vectorizeCompletedAt = vectorizeCompletedAt; }

    public Integer getVectorizeRetryCount() { return vectorizeRetryCount; }
    public void setVectorizeRetryCount(Integer vectorizeRetryCount) { this.vectorizeRetryCount = vectorizeRetryCount; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
}