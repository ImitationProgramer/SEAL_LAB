package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    // 최신 사진이 위로 오도록 정렬
    List<Gallery> findAllByOrderByUploadDateDesc();
}