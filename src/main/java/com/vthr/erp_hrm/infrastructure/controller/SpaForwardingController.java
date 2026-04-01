package com.vthr.erp_hrm.infrastructure.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Phục vụ React SPA: mọi route giao diện trả về {@code index.html}, React Router xử lý phía client.
 */
@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/",
            "/login",
            "/register",
            "/forbidden",
            "/verify-email",
            "/verify-otp",
            "/forgot-password",
            "/forgot-password/confirm",
            "/jobs",
            "/jobs/management",
            "/dashboard",
            "/candidate/applications",
            "/profile",
            "/admin/users",
            "/company/staff",
    })
    public String forwardSpaShell() {
        return "forward:/index.html";
    }

    @GetMapping("/jobs/{id:[0-9a-fA-F\\-]{36}}")
    public String forwardJobDetail() {
        return "forward:/index.html";
    }

    @GetMapping("/jobs/{id:[0-9a-fA-F\\-]{36}}/apply")
    public String forwardJobApply() {
        return "forward:/index.html";
    }

    @GetMapping("/jobs/{id:[0-9a-fA-F\\-]{36}}/kanban")
    public String forwardJobKanban() {
        return "forward:/index.html";
    }
}
