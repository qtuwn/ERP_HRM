package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.id = :id AND r.userId = :userId AND r.revoked = false")
    int revokeByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllTokensByUserId(UUID userId);
}
