package com.utfpr.ms_notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscriber_preference")
public class SubscriberPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(nullable = false, length = 100)
    private String category;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSubscriberId() { return subscriberId; }
    public void setSubscriberId(Long subscriberId) { this.subscriberId = subscriberId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
