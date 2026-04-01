package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.Skill;
import com.vthr.erp_hrm.core.model.SkillCategory;
import com.vthr.erp_hrm.core.repository.SkillCategoryRepository;
import com.vthr.erp_hrm.core.repository.SkillRepository;
import com.vthr.erp_hrm.core.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillDictionaryServiceImplTest {

    @Mock
    private SkillCategoryRepository categoryRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private AuditLogService auditLogService;

    private SkillDictionaryServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SkillDictionaryServiceImpl(categoryRepository, skillRepository, auditLogService);
    }

    @Test
    void createCategory_shouldRejectDuplicateName() {
        UUID actorId = UUID.randomUUID();
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.of(SkillCategory.builder()
                .id(UUID.randomUUID())
                .name("Backend")
                .build()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createCategory(actorId, "Backend"));
        assertEquals("Category name already exists", ex.getMessage());
    }

    @Test
    void createCategory_shouldTrimAndSaveAndAudit() {
        UUID actorId = UUID.randomUUID();
        when(categoryRepository.findByNameIgnoreCase("Backend")).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SkillCategory saved = service.createCategory(actorId, "  Backend  ");
        assertNotNull(saved.getId());
        assertEquals("Backend", saved.getName());
        verify(auditLogService).logAction(eq(actorId), eq("CREATE_SKILL_CATEGORY"), eq("SkillCategory"), eq(saved.getId()), eq("Backend"));
    }

    @Test
    void createSkill_shouldRejectBlankName() {
        UUID actorId = UUID.randomUUID();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createSkill(actorId, null, "   "));
        assertEquals("Name is required", ex.getMessage());
    }

    @Test
    void updateSkill_shouldUpdateCategoryAndName_andAudit() {
        UUID actorId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Skill existing = Skill.builder().id(skillId).categoryId(null).name("Old").build();
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(existing));
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(skillRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Skill saved = service.updateSkill(actorId, skillId, categoryId, "Java");
        assertEquals(categoryId, saved.getCategoryId());
        assertEquals("Java", saved.getName());
        verify(auditLogService).logAction(eq(actorId), eq("UPDATE_SKILL"), eq("Skill"), eq(skillId), eq("Java"));
    }
}

