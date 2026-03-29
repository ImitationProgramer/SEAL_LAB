package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublicationRepository extends JpaRepository<Publication, Long> {
    Optional<Publication> findByTitle(String title);
    // 기본적인 CRUD(저장, 삭제, 전체조회)는 이미 들어있습니다.

}