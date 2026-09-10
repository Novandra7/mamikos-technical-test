package com.mamikos.kostapi.kost.repository;

import com.mamikos.kostapi.kost.entity.Kost;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KostRepository extends JpaRepository<Kost, Long>, JpaSpecificationExecutor<Kost> {

    @EntityGraph(attributePaths = {"owner", "facilities", "photos"})
    @Query("select k from Kost k where k.id = :id and k.deletedAt is null and k.active = true")
    Optional<Kost> findActiveById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"owner", "facilities", "photos"})
    @Query("select k from Kost k where k.id = :id and k.deletedAt is null")
    Optional<Kost> findActiveOrInactiveById(@Param("id") Long id);

    @Query("select k from Kost k where k.owner.id = :ownerId and k.deletedAt is null")
    Page<Kost> findAllByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("select k from Kost k where k.owner.id = :ownerId")
    Page<Kost> findAllByOwnerIdIncludingDeleted(@Param("ownerId") Long ownerId, Pageable pageable);
}
