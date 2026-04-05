package com.vthr.erp_hrm.core.service;

import com.vthr.erp_hrm.core.model.Company;
import com.vthr.erp_hrm.core.model.CompanyMember;
import com.vthr.erp_hrm.core.model.Department;
import com.vthr.erp_hrm.core.model.User;

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

    List<Department> getDepartments(UUID companyId);
    Department createDepartment(UUID companyId, String name);
    Department getDepartmentById(UUID departmentId);
    void deleteDepartment(UUID departmentId);

    User createHrAccount(UUID companyId, String email, String password, String fullName, UUID departmentId);
}
