package com.mfish.erp.tournee;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArretTourneeRepository extends JpaRepository<ArretTournee, UUID> {

    List<ArretTournee> findByTourneeIdOrderByPositionAsc(UUID tourneeId);
}
