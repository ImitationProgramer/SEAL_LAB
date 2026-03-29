package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    // JpaRepository를 상속받는 것만으로도 save(), findAll(), delete() 등을 사용할 수 있습니다.
}