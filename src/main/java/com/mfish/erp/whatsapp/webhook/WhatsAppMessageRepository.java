package com.mfish.erp.whatsapp.webhook;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {

    boolean existsByWamid(String wamid);

    Optional<WhatsAppMessage> findByWamid(String wamid);
}
