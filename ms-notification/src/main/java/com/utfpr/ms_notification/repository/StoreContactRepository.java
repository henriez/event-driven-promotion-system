package com.utfpr.ms_notification.repository;

import com.utfpr.ms_notification.entity.StoreContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StoreContactRepository extends JpaRepository<StoreContact, String> {
    Optional<StoreContact> findByStoreId(String storeId);
}
