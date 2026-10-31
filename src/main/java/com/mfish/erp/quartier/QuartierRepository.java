package com.mfish.erp.quartier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuartierRepository extends JpaRepository<Quartier, UUID> {

    Optional<Quartier> findByNom(String nom);

    List<Quartier> findByActifTrueOrderByOrdreDefautAsc();
}
