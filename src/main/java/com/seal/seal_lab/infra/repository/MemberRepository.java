package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}