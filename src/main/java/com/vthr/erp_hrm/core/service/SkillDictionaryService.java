package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Skill;
import com.vthr.erp_hrm.core.model.SkillCategory;

import java.util.List;
import java.util.UUID;

public interface SkillDictionaryService {
    List<SkillCategory> listCategories();

    SkillCategory createCategory(UUID actorId, String name);

    SkillCategory updateCategory(UUID actorId, UUID categoryId, String name);

    void deleteCategory(UUID actorId, UUID categoryId);

    List<Skill> listSkills(UUID categoryId);

    Skill createSkill(UUID actorId, UUID categoryId, String name);

    Skill updateSkill(UUID actorId, UUID skillId, UUID categoryId, String name);

    void deleteSkill(UUID actorId, UUID skillId);
}

