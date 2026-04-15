package com.codegym.smartphonemanagement.repository;

import com.codegym.smartphonemanagement.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByName(String name); 
}
