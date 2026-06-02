package com.utfpr.ms_notification.repository;

import com.utfpr.ms_notification.entity.SubscriberPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubscriberPreferenceRepository extends JpaRepository<SubscriberPreference, Long> {
    List<SubscriberPreference> findByCategory(String category);
}
