package com.mfish.erp.whatsapp.webhook;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {

    boolean existsByWamid(String wamid);

    Optional<WhatsAppMessage> findByWamid(String wamid);

    /** Messages d'un numero, du plus recent au plus ancien (historique de conversation). */
    List<WhatsAppMessage> findByTelephoneOrderByCreatedAtDesc(String telephone, Pageable pageable);
}
