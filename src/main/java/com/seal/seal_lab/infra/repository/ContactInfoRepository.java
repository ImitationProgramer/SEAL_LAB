package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.ContactInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactInfoRepository extends JpaRepository<ContactInfo, Long> {
    // 기본 CRUD 기능을 자동으로 상속받습니다.
    // 연락처 정보는 보통 1개만 관리하므로 별도의 쿼리 메서드는 필요하지 않습니다.
}