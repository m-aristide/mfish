package com.mfish.erp.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByTelephone(String telephone);

    List<Client> findByQuartierId(UUID quartierId);

    List<Client> findByArchiveFalseOrderByNomAsc();

    @Query("SELECT c FROM Client c WHERE c.archive = false "
            + "AND (:recherche IS NULL "
            + "     OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :recherche, '%')) "
            + "     OR LOWER(c.telephone) LIKE LOWER(CONCAT('%', :recherche, '%'))) "
            + "ORDER BY c.nom ASC")
    Page<Client> rechercherActifs(String recherche, Pageable pageable);
}
