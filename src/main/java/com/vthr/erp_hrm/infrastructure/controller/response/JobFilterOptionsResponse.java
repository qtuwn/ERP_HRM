package com.vthr.erp_hrm.infrastructure.controller.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobFilterOptionsResponse {
    private List<String> cities;
    private List<String> industries;
    private List<String> jobTypes;
    private List<String> levels;
}
