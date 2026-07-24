package com.project.repository.mysql;

import com.project.entity.mysql.EmergencyResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyResourceRepository extends JpaRepository<EmergencyResource, Long> {

    Optional<EmergencyResource> findByResourceId(String resourceId);

    List<EmergencyResource> findByStatus(String status);

    List<EmergencyResource> findByResourceType(String resourceType);

    List<EmergencyResource> findByAvailableStockGreaterThan(Integer availableStock);

    @Modifying
    @Query("UPDATE EmergencyResource r SET r.availableStock = r.availableStock - :quantity, r.lockedStock = r.lockedStock + :quantity WHERE r.resourceId = :resourceId AND r.availableStock >= :quantity")
    int lockResource(@Param("resourceId") String resourceId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE EmergencyResource r SET r.availableStock = r.availableStock + :quantity, r.lockedStock = r.lockedStock - :quantity WHERE r.resourceId = :resourceId AND r.lockedStock >= :quantity")
    int releaseResource(@Param("resourceId") String resourceId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE EmergencyResource r SET r.lockedStock = r.lockedStock - :quantity WHERE r.resourceId = :resourceId AND r.lockedStock >= :quantity")
    int allocateResource(@Param("resourceId") String resourceId, @Param("quantity") Integer quantity);
}