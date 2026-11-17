package com.mfish.erp.connaissance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnaissanceRepository extends JpaRepository<Connaissance, UUID> {

    Optional<Connaissance> findTopByOrderByCreatedAtDesc();

    List<Connaissance> findAllByOrderByCreatedAtDesc();
}
