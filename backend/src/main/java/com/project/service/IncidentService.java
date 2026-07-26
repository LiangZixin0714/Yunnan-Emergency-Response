package com.project.service;

import com.project.config.MinIOConfig;
import com.project.dto.incident.IncidentReportRequest;
import com.project.dto.incident.IncidentReportResponse;
import com.project.dto.incident.IncidentRequest;
import com.project.dto.incident.IncidentResponse;
import com.project.entity.mysql.Incident;
import com.project.repository.mysql.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_IMAGES = 5;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    private final IncidentRepository incidentRepository;
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;
    private final AmapGeocodeService amapGeocodeService;

    public IncidentService(IncidentRepository incidentRepository, ObjectMapper objectMapper,
                          MinioClient minioClient, MinIOConfig minIOConfig,
                          AmapGeocodeService amapGeocodeService) {
        this.incidentRepository = incidentRepository;
        this.objectMapper = objectMapper;
        this.minioClient = minioClient;
        this.minIOConfig = minIOConfig;
        this.amapGeocodeService = amapGeocodeService;
    }

    @Transactional("mysqlTransactionManager")
    public IncidentReportResponse reportIncident(IncidentReportRequest request, Long reporterId) {
        logger.info("=== 接收到灾情上报请求 ===");
        logger.info("incidentName: {}", request.getIncidentName());
        logger.info("disasterType: {}", request.getDisasterType());
        logger.info("images: {}", request.getImages() != null ? request.getImages().length : 0);
        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().length; i++) {
                MultipartFile file = request.getImages()[i];
                logger.info("  image[{}]: name={}, size={}", i, file.getOriginalFilename(), file.getSize());
            }
        }
        
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID().toString());
        incident.setIncidentName(request.getIncidentName());
        incident.setTitle(request.getIncidentName());
        incident.setDisasterType(request.getDisasterType());
        incident.setIncidentLevel(request.getIncidentLevel());
        incident.setOccurTime(request.getOccurTime());
        incident.setLocation(request.getLocation());
        incident.setDescription(request.getDescription());
        incident.setDeathCount(request.getDeathCount());
        incident.setPropertyLoss(request.getPropertyLoss());
        incident.setStatus("processing");
        incident.setReporterId(reporterId);

        if (request.getLatitude() != null && request.getLongitude() != null) {
            incident.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            incident.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        } else if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            try {
                double[] coords = amapGeocodeService.geocode(request.getLocation());
                if (coords != null) {
                    incident.setLatitude(BigDecimal.valueOf(coords[0]));
                    incident.setLongitude(BigDecimal.valueOf(coords[1]));
                }
            } catch (Exception e) {
                logger.warn("地理编码失败，不影响上报流程: location={}", request.getLocation(), e);
            }
        }

        List<String> imageUrls = new ArrayList<>();
        if (request.getImages() != null && request.getImages().length > 0) {
            int count = Math.min(request.getImages().length, MAX_IMAGES);
            for (int i = 0; i < count; i++) {
                MultipartFile file = request.getImages()[i];
                if (!file.isEmpty()) {
                    String url = saveImage(file, incident.getIncidentId());
                    if (url != null) {
                        imageUrls.add(url);
                    }
                }
            }
        }

        try {
            incident.setImageUrls(objectMapper.writeValueAsString(imageUrls));
        } catch (Exception e) {
            logger.warn("Failed to serialize imageUrls", e);
        }

        incidentRepository.save(incident);

        return new IncidentReportResponse(incident.getIncidentId(), imageUrls);
    }

    @Transactional("mysqlTransactionManager")
    public Incident updateStatus(String incidentId, String status) {
        Incident incident = incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("灾情不存在，incidentId: " + incidentId));

        if ("completed".equals(status)) {
            String disposalStatus = incident.getDisposalPlanStatus();
            String resourceStatus = incident.getResourceDispatchStatus();
            if (!"accepted".equals(disposalStatus)) {
                throw new IllegalStateException(
                        "无法结束灾情：处置方案尚未被接受（当前状态: " + disposalStatus + "）");
            }
            if (!"completed".equals(resourceStatus)) {
                throw new IllegalStateException(
                        "无法结束灾情：资源调度尚未完成（当前状态: " + resourceStatus + "）");
            }
        }

        incident.setStatus(status);
        return incidentRepository.save(incident);
    }

    @Transactional("mysqlTransactionManager")
    public Incident completeResourceDispatch(String incidentId) {
        Incident incident = incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("灾情不存在，incidentId: " + incidentId));
        incident.setResourceDispatchStatus("completed");
        return incidentRepository.save(incident);
    }

    @Transactional("mysqlTransactionManager")
    public Incident completeIncident(String incidentId) {
        Incident incident = incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("灾情不存在，incidentId: " + incidentId));

        if ("completed".equals(incident.getStatus())) {
            throw new IllegalStateException("事件已结束");
        }

        String disposalStatus = incident.getDisposalPlanStatus();
        if (!"accepted".equals(disposalStatus)) {
            throw new IllegalStateException(
                    "无法结束灾情：处置方案尚未被接受（当前状态: " + disposalStatus + "）");
        }

        incident.setResourceDispatchStatus("completed");
        incident.setStatus("completed");
        return incidentRepository.save(incident);
    }

    private String saveImage(MultipartFile file, String incidentId) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                logger.warn("File has no name, skipping");
                return null;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                logger.warn("File size exceeds limit: {} > {} bytes", file.getSize(), MAX_FILE_SIZE);
                throw new IllegalArgumentException("图片大小不能超过10MB");
            }

            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            boolean validExtension = false;
            for (String ext : ALLOWED_EXTENSIONS) {
                if (ext.equalsIgnoreCase(extension)) {
                    validExtension = true;
                    break;
                }
            }
            if (!validExtension) {
                logger.warn("Invalid file extension: {}", extension);
                throw new IllegalArgumentException("不支持的图片格式，仅支持jpg/jpeg/png/gif/webp");
            }

            String contentType = file.getContentType();
            if (contentType != null) {
                boolean validContentType = false;
                for (String ct : ALLOWED_CONTENT_TYPES) {
                    if (ct.equalsIgnoreCase(contentType)) {
                        validContentType = true;
                        break;
                    }
                }
                if (!validContentType) {
                    logger.warn("Invalid content type: {}", contentType);
                    throw new IllegalArgumentException("图片类型不匹配，请上传有效图片文件");
                }
            }

            String dateDir = LocalDateTime.now().format(DATE_FORMATTER);
            String newFilename = incidentId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            String objectKey = dateDir + "/" + newFilename;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOConfig.getBucket())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType != null ? contentType : "image/jpeg")
                            .build()
            );

            String url = "/api/image/" + objectKey;
            logger.info("Image saved to MinIO: {}", url);
            return url;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save image to MinIO", e);
            throw new RuntimeException("图片上传失败", e);
        }
    }

    @Transactional("mysqlTransactionManager")
    public Incident getIncidentById(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("灾情不存在"));
    }

    public Map<String, Object> listIncidents(Integer page, Integer size, String disasterType,
                                              String incidentLevel, String status, String keyword) {
        List<Incident> incidents = incidentRepository.findAll();
        
        if (disasterType != null && !disasterType.isEmpty()) {
            incidents = incidents.stream().filter(i -> disasterType.equals(i.getDisasterType())).toList();
        }
        if (incidentLevel != null && !incidentLevel.isEmpty()) {
            incidents = incidents.stream().filter(i -> incidentLevel.equals(i.getIncidentLevel())).toList();
        }
        if (status != null && !status.isEmpty()) {
            incidents = incidents.stream().filter(i -> status.equals(i.getStatus())).toList();
        }
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            incidents = incidents.stream()
                    .filter(i -> i.getIncidentName().toLowerCase().contains(kw) ||
                            (i.getDescription() != null && i.getDescription().toLowerCase().contains(kw)) ||
                            (i.getLocation() != null && i.getLocation().toLowerCase().contains(kw)))
                    .toList();
        }

        int total = incidents.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        
        List<Incident> pageList = start < total ? incidents.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("list", pageList);
        return result;
    }

    @Transactional("mysqlTransactionManager")
    public IncidentResponse submitIncident(IncidentRequest request, Long reporterId) {
        Incident incident = new Incident();
        incident.setIncidentId(UUID.randomUUID().toString());
        incident.setIncidentName(request.getTitle());
        incident.setDisasterType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setStatus("processing");
        incident.setReporterId(reporterId);

        incidentRepository.save(incident);

        IncidentResponse response = new IncidentResponse();
        response.setIncidentId(incident.getIncidentId());
        response.setTitle(incident.getIncidentName());
        response.setDescription(incident.getDescription());
        response.setIncidentType(incident.getDisasterType());
        response.setStatus(incident.getStatus());
        response.setReportTime(incident.getReportTime());
        response.setCreatedAt(incident.getCreatedAt());

        return response;
    }

    @Transactional("mysqlTransactionManager")
    public Map<String, Object> backfillCoordinates() {
        List<Incident> allIncidents = incidentRepository.findAll();
        int total = 0;
        int updated = 0;
        int failed = 0;

        for (Incident incident : allIncidents) {
            if (incident.getLatitude() != null && incident.getLongitude() != null) {
                continue;
            }
            if (incident.getLocation() == null || incident.getLocation().trim().isEmpty()) {
                continue;
            }

            total++;
            try {
                double[] coords = amapGeocodeService.geocode(incident.getLocation());
                if (coords != null) {
                    incident.setLatitude(BigDecimal.valueOf(coords[0]));
                    incident.setLongitude(BigDecimal.valueOf(coords[1]));
                    incidentRepository.save(incident);
                    updated++;
                    logger.info("补全坐标成功: incidentId={}, location={}, lat={}, lng={}",
                            incident.getIncidentId(), incident.getLocation(), coords[0], coords[1]);
                } else {
                    failed++;
                    logger.warn("补全坐标失败（无结果）: incidentId={}, location={}",
                            incident.getIncidentId(), incident.getLocation());
                }
            } catch (Exception e) {
                failed++;
                logger.error("补全坐标异常: incidentId={}, location={}",
                        incident.getIncidentId(), incident.getLocation(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("updated", updated);
        result.put("failed", failed);
        return result;
    }
}