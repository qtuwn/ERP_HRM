package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.infrastructure.persistence.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {
    Page<JobEntity> findByStatus(String status, Pageable pageable);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE j.status = :status
            AND (
              LOWER(j.title) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.companyName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.city, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<JobEntity> findByStatusAndKeyword(@Param("status") String status, @Param("q") String q, Pageable pageable);

    /**
     * q/city/industry/jobType/level/skill: chuỗi rỗng = không lọc theo trường đó.
     * Tìm theo từ khóa: khớp title, company, city, department, industry, kỹ năng (requiredSkills/tags).
     */
    @Query("""
            SELECT j FROM JobEntity j
            WHERE j.status = :status
            AND (
              :q = ''
              OR LOWER(j.title) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.companyName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.city, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.industry, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.requiredSkills, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(j.tags, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:city = '' OR LOWER(TRIM(COALESCE(j.city, ''))) = LOWER(TRIM(:city)))
            AND (:industry = '' OR LOWER(TRIM(COALESCE(j.industry, ''))) = LOWER(TRIM(:industry)))
            AND (:jobType = '' OR j.jobType = :jobType)
            AND (:level = '' OR j.level = :level)
            AND (
              :skill = ''
              OR LOWER(COALESCE(j.requiredSkills, '')) LIKE LOWER(CONCAT('%', :skill, '%'))
              OR LOWER(COALESCE(j.tags, '')) LIKE LOWER(CONCAT('%', :skill, '%'))
            )
            """)
    Page<JobEntity> searchOpenJobs(
            @Param("status") String status,
            @Param("q") String q,
            @Param("city") String city,
            @Param("industry") String industry,
            @Param("jobType") String jobType,
            @Param("level") String level,
            @Param("skill") String skill,
            Pageable pageable);

    @Query("SELECT DISTINCT j.city FROM JobEntity j WHERE j.status = :status AND j.city IS NOT NULL AND TRIM(j.city) <> '' ORDER BY j.city")
    List<String> findDistinctCitiesByStatus(@Param("status") String status);

    @Query("SELECT DISTINCT j.industry FROM JobEntity j WHERE j.status = :status AND j.industry IS NOT NULL AND TRIM(j.industry) <> '' ORDER BY j.industry")
    List<String> findDistinctIndustriesByStatus(@Param("status") String status);

    @Query("SELECT DISTINCT j.jobType FROM JobEntity j WHERE j.status = :status AND j.jobType IS NOT NULL AND TRIM(j.jobType) <> '' ORDER BY j.jobType")
    List<String> findDistinctJobTypesByStatus(@Param("status") String status);

    @Query("SELECT DISTINCT j.level FROM JobEntity j WHERE j.status = :status AND j.level IS NOT NULL AND TRIM(j.level) <> '' ORDER BY j.level")
    List<String> findDistinctLevelsByStatus(@Param("status") String status);

    Page<JobEntity> findByDepartment(String department, Pageable pageable);

    Page<JobEntity> findByCompanyId(UUID companyId, Pageable pageable);

    Page<JobEntity> findByCompanyIdAndCreatedBy(UUID companyId, UUID createdBy, Pageable pageable);

    java.util.List<JobEntity> findByStatusAndExpiresAtBefore(String status, java.time.ZonedDateTime expiresAt);

    long countByStatus(String status);
}
