package com.vthr.erp_hrm.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    ADMIN, COMPANY, HR, CANDIDATE;

    @JsonCreator
    public static Role fromString(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        if ("SUPER_ADMIN".equals(normalized) || "SUPERADMIN".equals(normalized)) {
            return ADMIN;
        }

        if ("HR_MANAGER".equals(normalized)) {
            return COMPANY;
        }

        return Role.valueOf(normalized);
    }
}
