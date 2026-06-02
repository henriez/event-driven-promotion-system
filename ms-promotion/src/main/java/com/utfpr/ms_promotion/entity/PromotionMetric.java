package com.utfpr.ms_promotion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_metrics")
public class PromotionMetric {

    @Id
    @Column(name = "promotion_id")
    private Long promotionId;

    private Integer upvotes = 0;

    @Column(name = "heat_score", precision = 8, scale = 4)
    private BigDecimal heatScore = BigDecimal.ZERO;

    @Column(name = "last_calculated", insertable = false)
    private LocalDateTime lastCalculated;

    public Long getPromotionId() { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
    public Integer getUpvotes() { return upvotes; }
    public void setUpvotes(Integer upvotes) { this.upvotes = upvotes; }
    public BigDecimal getHeatScore() { return heatScore; }
    public void setHeatScore(BigDecimal heatScore) { this.heatScore = heatScore; }
    public LocalDateTime getLastCalculated() { return lastCalculated; }
    public void setLastCalculated(LocalDateTime lastCalculated) { this.lastCalculated = lastCalculated; }
}
