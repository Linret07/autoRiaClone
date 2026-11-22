package com.autoriaclone.repository;

import com.autoriaclone.entity.Role;
import com.autoriaclone.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
