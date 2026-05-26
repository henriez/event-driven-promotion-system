package com.utfpr.ms_promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utfpr.ms_promotion.entity.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}