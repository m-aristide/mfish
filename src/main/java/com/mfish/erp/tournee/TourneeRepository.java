package com.mfish.erp.tournee;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourneeRepository extends JpaRepository<Tournee, UUID> {

    Optional<Tournee> findByDate(LocalDate date);
}
