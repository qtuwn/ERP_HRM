package com.vthr.erp_hrm.infrastructure.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Truy vấn native: thread inbox HR — ứng viên + tin nhắn cuối, sắp xếp theo hoạt động gần nhất.
 */
@Repository
@RequiredArgsConstructor
public class RecruiterInboxNativeQuery {

    private final EntityManager entityManager;

    public long countByJobIds(List<UUID> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return 0;
        }
        Query q = entityManager.createNativeQuery(
                "SELECT count(*) FROM applications a WHERE a.job_id IN (:jobIds) AND COALESCE(a.status, '') <> 'WITHDRAWN'");
        q.setParameter("jobIds", jobIds);
        Object single = q.getSingleResult();
        if (single instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(single.toString());
    }

    @SuppressWarnings("unchecked")
    public List<RecruiterInboxRow> fetchByJobIds(List<UUID> jobIds, int offset, int limit) {
        if (jobIds == null || jobIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT a.id AS application_id,
                       a.job_id,
                       j.title AS job_title,
                       u.full_name AS candidate_name,
                       u.email AS candidate_email,
                       a.status,
                       lm.content AS last_content,
                       lm.created_at AS last_created_at
                FROM applications a
                JOIN jobs j ON j.id = a.job_id
                JOIN users u ON u.id = a.candidate_id
                LEFT JOIN LATERAL (
                    SELECT m.content, m.created_at
                    FROM messages m
                    WHERE m.application_id = a.id
                    ORDER BY m.created_at DESC
                    LIMIT 1
                ) lm ON true
                WHERE a.job_id IN (:jobIds)
                  AND COALESCE(a.status, '') <> 'WITHDRAWN'
                ORDER BY COALESCE(lm.created_at, a.updated_at) DESC
                OFFSET :offset LIMIT :limit
                """;
        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("jobIds", jobIds);
        q.setParameter("offset", offset);
        q.setParameter("limit", limit);
        List<Object[]> raw = q.getResultList();
        List<RecruiterInboxRow> out = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            out.add(new RecruiterInboxRow(
                    (UUID) row[0],
                    (UUID) row[1],
                    row[2] != null ? row[2].toString() : null,
                    row[3] != null ? row[3].toString() : null,
                    row[4] != null ? row[4].toString() : null,
                    row[5] != null ? row[5].toString() : null,
                    row[6] != null ? row[6].toString() : null,
                    RecruiterInboxRow.toZonedDateTime(row[7])));
        }
        return out;
    }
}
