package com.vthr.erp_hrm.infrastructure.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "redirect:/jobs";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/jobs")
    public String jobList() {
        return "public/job-list";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable UUID id) {
        return "public/job-detail";
    }

    @GetMapping("/jobs/{id}/apply")
    public String jobApply(@PathVariable UUID id) {
        return "candidate/apply";
    }

    @GetMapping("/jobs/{id}/kanban")
    public String kanbanBoard(@PathVariable UUID id) {
        return "hr/kanban-board";
    }

    @GetMapping("/candidate/applications")
    public String candidateApplications() {
        return "candidate/applications";
    }

    @GetMapping("/profile")
    public String userProfile() {
        return "candidate/profile";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "hr/admin-users";
    }
}
