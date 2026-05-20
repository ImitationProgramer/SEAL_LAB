package com.seal.seal_lab.infra.repository;
import com.seal.seal_lab.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String testAdmin);
    Optional<User> findByEmail(String email);
    List<User> findAllByName(String name);
}
