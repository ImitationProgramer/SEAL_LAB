package com.seal.seal_lab.infra.repository;
import com.seal.seal_lab.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginId(String testAdmin);
    // 로그인 ID로 사용자를 찾는 기능을 추가합니다.
}