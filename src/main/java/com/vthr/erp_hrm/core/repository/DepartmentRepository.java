package com.vthr.erp_hrm.core.repository;

import com.vthr.erp_hrm.core.model.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {
    List<Department> findByCompanyId(UUID companyId);

    Optional<Department> findById(UUID id);

    Optional<Department> findByCompanyIdAndName(UUID companyId, String name);

    Department save(Department department);

    void deleteById(UUID id);
}
