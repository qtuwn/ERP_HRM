package com.vthr.erp_hrm.infrastructure.persistence.repository;

import com.vthr.erp_hrm.core.model.AccountStatus;
import com.vthr.erp_hrm.core.model.Role;
import com.vthr.erp_hrm.core.model.User;
import com.vthr.erp_hrm.core.repository.UserRepository;
import com.vthr.erp_hrm.infrastructure.persistence.entity.UserEntity;
import com.vthr.erp_hrm.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper userMapper;

    @Override
    public List<User> findAll() {
        return jpaRepository.findByStatusNot(AccountStatus.DELETED).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findByStatusNot(AccountStatus.DELETED, pageable).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .filter(entity -> entity.getStatus() != AccountStatus.DELETED)
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return jpaRepository.findAllById(ids).stream()
                .filter(entity -> entity.getStatus() != AccountStatus.DELETED)
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmailAndStatusNot(email, AccountStatus.DELETED).map(userMapper::toDomain);
    }

    @Override
    public List<User> findByRole(Role role) {
        return jpaRepository.findByRoleAndStatusNot(role, AccountStatus.DELETED).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<User> findByRole(Role role, Pageable pageable) {
        return jpaRepository.findByRoleAndStatusNot(role, AccountStatus.DELETED, pageable).map(userMapper::toDomain);
    }

    @Override
    public List<User> findByCompanyId(UUID companyId) {
        return jpaRepository.findByCompanyIdAndStatusNot(companyId, AccountStatus.DELETED).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<User> findByCompanyId(UUID companyId, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndStatusNot(companyId, AccountStatus.DELETED, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public Page<User> findByCompanyIdAndRole(UUID companyId, Role role, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndRoleAndStatusNot(companyId, role, AccountStatus.DELETED, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public Page<User> findByCompanyIdAndRoleIn(UUID companyId, Collection<Role> roles, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndRoleInAndStatusNot(companyId, roles, AccountStatus.DELETED, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public Page<User> findByCompanyIdAndDepartmentId(UUID companyId, UUID departmentId, Pageable pageable) {
        return jpaRepository.findByCompanyIdAndDepartmentIdAndStatusNot(companyId, departmentId, AccountStatus.DELETED, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    public long countByRole(Role role) {
        return jpaRepository.countByRoleAndStatusNot(role, AccountStatus.DELETED);
    }

    @Override
    public Map<String, Long> countUsersGroupedByRole() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : jpaRepository.countGroupedByRole(AccountStatus.DELETED)) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String roleKey = String.valueOf(row[0]);
            long n = row[1] instanceof Number num ? num.longValue() : 0L;
            map.put(roleKey, n);
        }
        return map;
    }

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity savedEntity = jpaRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
