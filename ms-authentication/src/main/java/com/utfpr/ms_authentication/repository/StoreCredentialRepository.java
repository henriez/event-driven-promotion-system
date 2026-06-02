package com.utfpr.ms_authentication.repository;

import com.utfpr.ms_authentication.entity.StoreCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StoreCredentialRepository extends JpaRepository<StoreCredential, String> {
    Optional<StoreCredential> findByStoreIdAndIsActiveTrue(String storeId);
}
