package com.vthr.erp_hrm.infrastructure.persistence.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public record RecruiterInboxRow(
        UUID applicationId,
        UUID jobId,
        String jobTitle,
        String candidateName,
        String candidateEmail,
        String status,
        String lastMessageContent,
        ZonedDateTime lastMessageAt) {

    static ZonedDateTime toZonedDateTime(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof ZonedDateTime z) {
            return z;
        }
        if (o instanceof Instant i) {
            return i.atZone(ZoneId.systemDefault());
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atZone(ZoneId.systemDefault());
        }
        if (o instanceof java.time.OffsetDateTime od) {
            return od.toZonedDateTime();
        }
        return null;
    }
}
