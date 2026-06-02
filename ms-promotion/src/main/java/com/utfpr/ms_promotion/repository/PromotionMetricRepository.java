package com.utfpr.ms_promotion.repository;

import com.utfpr.ms_promotion.entity.PromotionMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionMetricRepository extends JpaRepository<PromotionMetric, Long> {
}
