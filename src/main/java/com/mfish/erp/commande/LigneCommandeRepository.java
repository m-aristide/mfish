package com.mfish.erp.commande;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, UUID> {

    List<LigneCommande> findByCommandeId(UUID commandeId);
}
