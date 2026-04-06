package com.vthr.erp_hrm.core.service.impl;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Company;
import com.vthr.erp_hrm.core.model.CompanyMember;
import com.vthr.erp_hrm.core.model.Department;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.DepartmentRepository;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.core.service.CompanyService;
import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyEntity;
import com.vthr.erp_hrm.infrastructure.persistence.entity.CompanyMemberEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.CompanyMapper;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.CompanyMemberMapper;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyMemberRepository;
import com.vthr.erp_hrm.infrastructure.persistence.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Company createOrGetCompany(String name) {
        return companyRepository.findByNameIgnoreCase(name)
                .map(CompanyMapper::toDomain)
                .orElseGet(() -> {
                    CompanyEntity newCompany = CompanyEntity.builder()
                            .id(UUID.randomUUID())
                            .name(name)
                            .isVerifiedByAdmin(false)
                            .build();
                    return CompanyMapper.toDomain(companyRepository.save(newCompany));
                });
    }

    @Override
    public Company getCompanyById(UUID id) {
        return companyRepository.findById(id)
                .map(CompanyMapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Company not found"));
    }

    @Override
    @Transactional
    public CompanyMember addHrMember(UUID companyId, UUID userId, String memberRole) {
        CompanyMemberEntity newMember = CompanyMemberEntity.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(userId)
                .memberRole(memberRole)
                .status("ACTIVE")
                .build();
        return CompanyMemberMapper.toDomain(companyMemberRepository.save(newMember));
    }

    @Override
    public List<CompanyMember> getCompanyMembers(UUID companyId) {
        return companyMemberRepository.findByCompanyId(companyId)
                .stream().map(CompanyMemberMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CompanyMember getMemberByUserId(UUID userId) {
        return companyMemberRepository.findByUserId(userId)
                .map(CompanyMemberMapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateMemberRole(UUID companyId, UUID userId, String newRole) {
        CompanyMemberEntity member = companyMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (!member.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Member does not belong to this company");
        }
        member.setMemberRole(newRole);
        companyMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void removeMember(UUID companyId, UUID userId) {
        CompanyMemberEntity member = companyMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (!member.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Member does not belong to this company");
        }
        companyMemberRepository.delete(member);
    }

    @Override
    public List<Department> getDepartments(UUID companyId) {
        return departmentRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional
    public Department createDepartment(UUID companyId, String name) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (departmentRepository.findByCompanyIdAndName(companyId, name).isPresent()) {
            throw new RuntimeException("Department '" + name + "' already exists in this company");
        }

        Department dept = Department.builder()
                .companyId(companyId)
                .name(name.trim())
                .build();
        return departmentRepository.save(dept);
    }

    @Override
    public Department getDepartmentById(UUID departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Override
    @Transactional
    public void deleteDepartment(UUID departmentId) {
        departmentRepository.deleteById(departmentId);
    }

    @Override
    @Transactional
    public User createHrAccount(UUID companyId, String email, String password, String fullName, UUID departmentId) {
        companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        String departmentName = null;
        if (departmentId != null) {
            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            if (!dept.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Department does not belong to this company");
            }
            departmentName = dept.getName();
        }

        User hrUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.HR)
                .isActive(true)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .verifiedAt(ZonedDateTime.now())
                .mustChangePassword(true)
                .fullName(fullName)
                .companyId(companyId)
                .departmentId(departmentId)
                .department(departmentName)
                .build();

        User saved = userRepository.save(hrUser);

        addHrMember(companyId, saved.getId(), Role.HR.name());

        return saved;
    }
}
