package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Interview;
import com.vthr.erp_hrm.core.service.InterviewService;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @Data
    public static class ScheduleInterviewRequest {
        private ZonedDateTime interviewTime;
        private String locationOrLink;
    }

    @Data
    public static class UpdateInterviewStatusRequest {
        private String status;
    }

    @PostMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Interview>> scheduleInterview(
            @PathVariable UUID applicationId,
            @RequestBody ScheduleInterviewRequest request,
            Authentication authentication) {
        
        UUID interviewerId = UUID.fromString(authentication.getName());
        Interview interview = interviewService.scheduleInterview(
                applicationId, 
                request.getInterviewTime(), 
                request.getLocationOrLink(), 
                interviewerId
        );
        return ResponseEntity.ok(ApiResponse.success(interview, "Interview scheduled and candidate notified."));
    }

    @GetMapping("/applications/{applicationId}/interviews")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'CANDIDATE')")
    public ResponseEntity<ApiResponse<List<Interview>>> getInterviews(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getInterviewsByApplication(applicationId), 
                "Fetched interviews successfully"
        ));
    }

    @PatchMapping("/interviews/{interviewId}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'COMPANY')")
    public ResponseEntity<ApiResponse<Interview>> updateInterviewStatus(
            @PathVariable UUID interviewId,
            @RequestBody UpdateInterviewStatusRequest request) {
        Interview interview = interviewService.updateInterviewStatus(interviewId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(interview, "Updated interview status effectively"));
    }
}
