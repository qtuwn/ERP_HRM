package com.vthr.erp_hrm.core.model;

import java.util.List;

public record PublicJobFilterOptions(
        List<String> cities,
        List<String> industries,
        List<String> jobTypes,
        List<String> levels
) {
}
