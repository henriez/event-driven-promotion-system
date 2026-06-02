package com.utfpr.ms_promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utfpr.ms_promotion.entity.Promotion;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByCategoryIgnoreCase(String category);

    @Query(value = """
        SELECT * FROM promotion p
        WHERE (:category IS NULL OR LOWER(p.category::text) = LOWER(CAST(:category AS text)))
        AND (:search IS NULL OR LOWER(p.title::text) LIKE LOWER('%' || CAST(:search AS text) || '%')
          OR LOWER(p.description) LIKE LOWER('%' || CAST(:search AS text) || '%'))
        ORDER BY p.created_at DESC
    """, nativeQuery = true)
    List<Promotion> findFiltered(@Param("category") String category, @Param("search") String search);
}