package com.mfish.erp.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvenementLogRepository extends JpaRepository<EvenementLog, UUID> {
}
