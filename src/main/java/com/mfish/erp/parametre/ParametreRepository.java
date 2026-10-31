package com.mfish.erp.parametre;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametreRepository extends JpaRepository<Parametre, String> {

    Optional<Parametre> findByCle(String cle);
}
