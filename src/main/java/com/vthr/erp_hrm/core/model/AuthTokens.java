package com.vthr.erp_hrm.core.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthTokens {
    private String accessToken;
    private String refreshToken;
    private User user;
}
