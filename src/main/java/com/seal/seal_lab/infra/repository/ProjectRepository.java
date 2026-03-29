package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    // 최신 과제가 위로 오게 하려면 정렬 메서드를 추가할 수 있습니다.
    List<Project> findAllByOrderByIdDesc();
}
