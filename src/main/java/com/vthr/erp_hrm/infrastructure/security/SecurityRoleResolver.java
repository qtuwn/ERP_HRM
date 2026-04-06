package com.vthr.erp_hrm.infrastructure.security;

import com.vthr.erp_hrm.core.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring Security có thể gắn nhiều authority (ROLE_USER, ROLE_HR, …). Không dùng {@code findFirst()}
 * vì thứ tự không đảm bảo — dễ gây sai role và lỗi "Access denied" dù đã đăng nhập đúng.
 */
public final class SecurityRoleResolver {

    private SecurityRoleResolver() {
    }

    public static Role resolveRole(Authentication authentication) {
        if (authentication == null) {
            return Role.CANDIDATE;
        }
        Role best = null;
        int bestPri = -1;
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            Role r;
            try {
                r = Role.fromString(ga.getAuthority());
            } catch (IllegalArgumentException | NullPointerException e) {
                continue;
            }
            if (r == null) {
                continue;
            }
            int pri = switch (r) {
                case ADMIN -> 4;
                case COMPANY -> 3;
                case HR -> 2;
                case CANDIDATE -> 1;
            };
            if (pri > bestPri) {
                bestPri = pri;
                best = r;
            }
        }
        return best != null ? best : Role.CANDIDATE;
    }
}
