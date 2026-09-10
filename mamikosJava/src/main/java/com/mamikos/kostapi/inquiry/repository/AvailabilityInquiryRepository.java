package com.mamikos.kostapi.inquiry.repository;

import com.mamikos.kostapi.inquiry.entity.AvailabilityInquiry;
import com.mamikos.kostapi.inquiry.entity.InquiryStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityInquiryRepository extends JpaRepository<AvailabilityInquiry, Long> {

    @EntityGraph(attributePaths = {"kost"})
    Page<AvailabilityInquiry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"kost", "user"})
    @Query("select i from AvailabilityInquiry i where i.kost.owner.id = :ownerId")
    Page<AvailabilityInquiry> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"kost", "user"})
    @Query("select i from AvailabilityInquiry i where i.kost.owner.id = :ownerId and i.status = :status")
    Page<AvailabilityInquiry> findByOwnerIdAndStatus(
            @Param("ownerId") Long ownerId, @Param("status") InquiryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"kost", "user"})
    @Query("select i from AvailabilityInquiry i where i.id = :id and i.kost.owner.id = :ownerId")
    Optional<AvailabilityInquiry> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    long countByKostId(Long kostId);
}
