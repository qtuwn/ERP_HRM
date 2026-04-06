package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.Department;
import com.vthr.erp_hrm.core.repository.DepartmentRepository;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private final DepartmentJpaRepository jpaRepository;

    @Override
    public List<Department> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(DepartmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Department> findById(UUID id) {
        return jpaRepository.findById(id).map(DepartmentMapper::toDomain);
    }

    @Override
    public Optional<Department> findByCompanyIdAndName(UUID companyId, String name) {
        return jpaRepository.findByCompanyIdAndNameIgnoreCase(companyId, name)
                .map(DepartmentMapper::toDomain);
    }

    @Override
    public Department save(Department department) {
        var entity = DepartmentMapper.toEntity(department);
        return DepartmentMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
