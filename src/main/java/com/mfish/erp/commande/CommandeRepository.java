package com.mfish.erp.commande;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandeRepository extends JpaRepository<Commande, UUID> {

    List<Commande> findByStatut(StatutCommande statut);

    List<Commande> findByDateLivraisonSouhaitee(LocalDate date);

    List<Commande> findByClientId(UUID clientId);
}
