package com.project.repository.mysql;

import com.project.entity.mysql.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    Optional<Knowledge> findByFileId(String fileId);

    void deleteByFileId(String fileId);

    List<Knowledge> findByVectorizeStatusAndVectorizeRetryCountLessThan(String status, Integer maxRetryCount);
}