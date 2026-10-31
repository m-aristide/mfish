package com.mfish.erp.notification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByCommandeIdAndType(UUID commandeId, TypeNotification type);
}
