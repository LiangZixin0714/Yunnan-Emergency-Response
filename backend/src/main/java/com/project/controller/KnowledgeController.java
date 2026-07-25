package com.project.controller;

import com.project.common.Result;
import com.project.entity.mysql.Knowledge;
import com.project.service.KnowledgeService;
import com.project.service.VectorizeService;
import com.project.repository.mysql.KnowledgeRepository;
import io.minio.GetObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeService knowledgeService;
    private final VectorizeService vectorizeService;
    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeController(KnowledgeService knowledgeService, VectorizeService vectorizeService,
                               KnowledgeRepository knowledgeRepository) {
        this.knowledgeService = knowledgeService;
        this.vectorizeService = vectorizeService;
        this.knowledgeRepository = knowledgeRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<Knowledge>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        String username = authentication.getName();
        Long uploaderId = 1L;
        Knowledge knowledge = knowledgeService.uploadFile(file, description, uploaderId, username);
        return ResponseEntity.ok(Result.success(knowledge));
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<Knowledge>>> listFiles() {
        List<Knowledge> files = knowledgeService.listFiles();
        return ResponseEntity.ok(Result.success(files));
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<Result<Void>> deleteFile(@PathVariable String fileId) {
        knowledgeService.deleteFile(fileId);
        return ResponseEntity.ok(Result.success(null));
    }

    @PostMapping("/vectorize/retry/{fileId}")
    public ResponseEntity<Result<Void>> retryVectorize(@PathVariable String fileId) {
        Knowledge knowledge = knowledgeRepository.findByFileId(fileId)
                .orElse(null);
        if (knowledge == null) {
            return ResponseEntity.ok(Result.badRequest("文件不存在"));
        }
        if (!Knowledge.STATUS_FAILED.equals(knowledge.getVectorizeStatus())) {
            return ResponseEntity.ok(Result.badRequest("仅失败状态可重新处理"));
        }
        knowledge.setVectorizeStatus(Knowledge.STATUS_PENDING);
        knowledge.setVectorizeRetryCount(0);
        knowledge.setVectorizeFailReason(null);
        knowledge.setVectorizeStartedAt(null);
        knowledge.setVectorizeCompletedAt(null);
        knowledgeRepository.save(knowledge);

        vectorizeService.triggerVectorizeAsync(fileId);
        return ResponseEntity.ok(Result.success(null));
    }

    @GetMapping("/download/{fileId}")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) {
        try (GetObjectResponse inputStream = knowledgeService.downloadFile(fileId)) {
            Knowledge knowledge = knowledgeService.listFiles().stream()
                    .filter(k -> k.getFileId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("文件不存在"));

            String encodedFilename = URLEncoder.encode(knowledge.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFilename);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(knowledge.getFileSize()));

            try (OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (IllegalArgumentException e) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            } catch (Exception ignored) {}
        } catch (Exception e) {
            logger.error("Failed to download file: {}", fileId, e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件下载失败");
            } catch (Exception ignored) {}
        }
    }
}