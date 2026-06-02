package com.utfpr.ms_notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_contact")
public class StoreContact {

    @Id
    @Column(name = "store_id", length = 100)
    private String storeId;

    @Column(nullable = false)
    private String email;

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
