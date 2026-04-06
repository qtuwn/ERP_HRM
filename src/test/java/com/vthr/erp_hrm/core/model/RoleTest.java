package com.vthr.erp_hrm.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoleTest {

    @Test
    void fromString_shouldHandleNull() {
        assertNull(Role.fromString(null));
    }

    @Test
    void fromString_shouldStripRolePrefixAndIgnoreCase() {
        assertEquals(Role.HR, Role.fromString("ROLE_HR"));
        assertEquals(Role.COMPANY, Role.fromString("role_company"));
        assertEquals(Role.CANDIDATE, Role.fromString(" candidate "));
    }

    @Test
    void fromString_shouldMapLegacyAliases() {
        assertEquals(Role.ADMIN, Role.fromString("SUPER_ADMIN"));
        assertEquals(Role.ADMIN, Role.fromString("superadmin"));
        assertEquals(Role.COMPANY, Role.fromString("HR_MANAGER"));
    }
}

