package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Company;
import com.vthr.erp_hrm.core.model.CompanyMember;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    Company createOrGetCompany(String name);
    Company getCompanyById(UUID id);
    CompanyMember addHrMember(UUID companyId, UUID userId, String memberRole);
    List<CompanyMember> getCompanyMembers(UUID companyId);
    CompanyMember getMemberByUserId(UUID userId);
    void updateMemberRole(UUID companyId, UUID userId, String newRole);
    void removeMember(UUID companyId, UUID userId);
}
