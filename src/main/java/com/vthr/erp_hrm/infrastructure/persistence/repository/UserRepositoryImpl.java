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

import java.util.List;
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
    public long countByRole(Role role) {
        return jpaRepository.countByRoleAndStatusNot(role, AccountStatus.DELETED);
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
