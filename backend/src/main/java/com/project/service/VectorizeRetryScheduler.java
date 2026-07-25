package com.project.service;

import com.project.entity.mysql.Knowledge;
import com.project.repository.mysql.KnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
public class VectorizeRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VectorizeRetryScheduler.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final KnowledgeRepository knowledgeRepository;
    private final VectorizeService vectorizeService;

    public VectorizeRetryScheduler(KnowledgeRepository knowledgeRepository,
                                   VectorizeService vectorizeService) {
        this.knowledgeRepository = knowledgeRepository;
        this.vectorizeService = vectorizeService;
    }

    @Scheduled(fixedDelay = 30000)
    public void retryFailedVectorizations() {
        List<Knowledge> failedRecords = knowledgeRepository
                .findByVectorizeStatusAndVectorizeRetryCountLessThan(Knowledge.STATUS_FAILED, MAX_RETRY_COUNT);

        if (failedRecords.isEmpty()) {
            return;
        }

        logger.info("向量化自动重试扫描: 发现{}条失败记录", failedRecords.size());

        for (Knowledge knowledge : failedRecords) {
            if (knowledge.getVectorizeCompletedAt() == null) {
                continue;
            }

            int retryCount = knowledge.getVectorizeRetryCount();
            long retryIntervalSeconds = 30L * (1L << (retryCount - 1));
            LocalDateTime nextRetryTime = knowledge.getVectorizeCompletedAt().plusSeconds(retryIntervalSeconds);

            if (LocalDateTime.now().isAfter(nextRetryTime)) {
                logger.info("向量化自动重试: fileId={}, retryCount={}", knowledge.getFileId(), retryCount);
                vectorizeService.triggerVectorizeWithRetryCheck(knowledge.getFileId());
            }
        }
    }
}