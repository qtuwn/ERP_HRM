package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Skill;
import com.vthr.erp_hrm.core.model.SkillCategory;
import com.vthr.erp_hrm.core.repository.SkillCategoryRepository;
import com.vthr.erp_hrm.core.repository.SkillRepository;
import com.vthr.erp_hrm.core.service.AuditLogService;
import com.vthr.erp_hrm.core.service.SkillDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillDictionaryServiceImpl implements SkillDictionaryService {
    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final AuditLogService auditLogService;

    @Override
    public List<SkillCategory> listCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(SkillCategory::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public SkillCategory createCategory(UUID actorId, String name) {
        String normalized = requireName(name);
        categoryRepository.findByNameIgnoreCase(normalized).ifPresent(x -> {
            throw new RuntimeException("Category name already exists");
        });
        SkillCategory saved = categoryRepository.save(SkillCategory.builder()
                .id(UUID.randomUUID())
                .name(normalized)
                .build());
        auditLogService.logAction(actorId, "CREATE_SKILL_CATEGORY", "SkillCategory", saved.getId(), normalized);
        return saved;
    }

    @Override
    @Transactional
    public SkillCategory updateCategory(UUID actorId, UUID categoryId, String name) {
        String normalized = requireName(name);
        SkillCategory existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.findByNameIgnoreCase(normalized).ifPresent(x -> {
            if (!x.getId().equals(categoryId)) {
                throw new RuntimeException("Category name already exists");
            }
        });
        existing.setName(normalized);
        SkillCategory saved = categoryRepository.save(existing);
        auditLogService.logAction(actorId, "UPDATE_SKILL_CATEGORY", "SkillCategory", categoryId, normalized);
        return saved;
    }

    @Override
    @Transactional
    public void deleteCategory(UUID actorId, UUID categoryId) {
        categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.deleteById(categoryId);
        auditLogService.logAction(actorId, "DELETE_SKILL_CATEGORY", "SkillCategory", categoryId, "Deleted");
    }

    @Override
    public List<Skill> listSkills(UUID categoryId) {
        if (categoryId == null) {
            return skillRepository.findAll();
        }
        return skillRepository.findByCategoryId(categoryId);
    }

    @Override
    @Transactional
    public Skill createSkill(UUID actorId, UUID categoryId, String name) {
        String normalized = requireName(name);
        skillRepository.findByNameIgnoreCase(normalized).ifPresent(x -> {
            throw new RuntimeException("Skill name already exists");
        });
        Skill saved = skillRepository.save(Skill.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .name(normalized)
                .build());
        auditLogService.logAction(actorId, "CREATE_SKILL", "Skill", saved.getId(), normalized);
        return saved;
    }

    @Override
    @Transactional
    public Skill updateSkill(UUID actorId, UUID skillId, UUID categoryId, String name) {
        String normalized = requireName(name);
        Skill existing = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        skillRepository.findByNameIgnoreCase(normalized).ifPresent(x -> {
            if (!x.getId().equals(skillId)) {
                throw new RuntimeException("Skill name already exists");
            }
        });
        existing.setName(normalized);
        existing.setCategoryId(categoryId);
        Skill saved = skillRepository.save(existing);
        auditLogService.logAction(actorId, "UPDATE_SKILL", "Skill", skillId, normalized);
        return saved;
    }

    @Override
    @Transactional
    public void deleteSkill(UUID actorId, UUID skillId) {
        skillRepository.findById(skillId).orElseThrow(() -> new RuntimeException("Skill not found"));
        skillRepository.deleteById(skillId);
        auditLogService.logAction(actorId, "DELETE_SKILL", "Skill", skillId, "Deleted");
    }

    private static String requireName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("Name is required");
        }
        String normalized = raw.trim();
        if (normalized.length() > 255) {
            throw new RuntimeException("Name is too long");
        }
        return normalized;
    }
}

