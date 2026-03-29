package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    // JpaRepository를 상속받는 것만으로도 save(), findById(), findAll() 등의
    // 기본 CRUD 기능을 별도의 구현 없이 사용할 수 있습니다.
}