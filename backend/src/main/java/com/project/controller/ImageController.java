package com.project.controller;

import com.project.config.MinIOConfig;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;

    public ImageController(MinioClient minioClient, MinIOConfig minIOConfig) {
        this.minioClient = minioClient;
        this.minIOConfig = minIOConfig;
    }

    @GetMapping("/{dateDir}/{filename}")
    public void getImage(@PathVariable String dateDir, @PathVariable String filename, HttpServletResponse response) {
        String objectKey = dateDir + "/" + filename;
        
        try (GetObjectResponse inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minIOConfig.getBucket())
                        .object(objectKey)
                        .build())) {
            
            String contentType = getContentType(filename);
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=86400");
            
            try (OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            
            logger.debug("Image served from MinIO: {}", objectKey);
            
        } catch (MinioException e) {
            logger.error("MinIO error when fetching image: {}", objectKey, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException e) {
            logger.error("IO error when serving image: {}", objectKey, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Error when serving image: {}", objectKey, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String getContentType(String filename) {
        String lowercase = filename.toLowerCase();
        if (lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowercase.endsWith(".png")) {
            return "image/png";
        } else if (lowercase.endsWith(".gif")) {
            return "image/gif";
        } else if (lowercase.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
