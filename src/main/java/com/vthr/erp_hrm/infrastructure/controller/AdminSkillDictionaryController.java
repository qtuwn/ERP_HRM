package com.vthr.erp_hrm.infrastructure.controller;

import com.vthr.erp_hrm.core.model.Skill;
import com.vthr.erp_hrm.core.model.SkillCategory;
import com.vthr.erp_hrm.core.service.SkillDictionaryService;
import com.vthr.erp_hrm.infrastructure.controller.request.AdminUpsertSkillCategoryRequest;
import com.vthr.erp_hrm.infrastructure.controller.request.AdminUpsertSkillRequest;
import com.vthr.erp_hrm.infrastructure.controller.response.ApiResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.SkillCategoryResponse;
import com.vthr.erp_hrm.infrastructure.controller.response.SkillResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/skill-dictionary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSkillDictionaryController {
    private final SkillDictionaryService service;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<SkillCategoryResponse>>> listCategories() {
        List<SkillCategoryResponse> data = service.listCategories().stream().map(SkillCategoryResponse::fromDomain).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Fetched categories successfully"));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<SkillCategoryResponse>> createCategory(
            @Valid @RequestBody AdminUpsertSkillCategoryRequest request,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        SkillCategory saved = service.createCategory(actorId, request.getName());
        return ResponseEntity.ok(ApiResponse.success(SkillCategoryResponse.fromDomain(saved), "Created category successfully"));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<SkillCategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody AdminUpsertSkillCategoryRequest request,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        SkillCategory saved = service.updateCategory(actorId, categoryId, request.getName());
        return ResponseEntity.ok(ApiResponse.success(SkillCategoryResponse.fromDomain(saved), "Updated category successfully"));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        service.deleteCategory(actorId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted category successfully"));
    }

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> listSkills(@RequestParam(required = false) UUID categoryId) {
        List<SkillResponse> data = service.listSkills(categoryId).stream().map(SkillResponse::fromDomain).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Fetched skills successfully"));
    }

    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody AdminUpsertSkillRequest request,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        Skill saved = service.createSkill(actorId, request.getCategoryId(), request.getName());
        return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromDomain(saved), "Created skill successfully"));
    }

    @PutMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable UUID skillId,
            @Valid @RequestBody AdminUpsertSkillRequest request,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        Skill saved = service.updateSkill(actorId, skillId, request.getCategoryId(), request.getName());
        return ResponseEntity.ok(ApiResponse.success(SkillResponse.fromDomain(saved), "Updated skill successfully"));
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(
            @PathVariable UUID skillId,
            Authentication authentication
    ) {
        UUID actorId = UUID.fromString(authentication.getName());
        service.deleteSkill(actorId, skillId);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted skill successfully"));
    }
}

