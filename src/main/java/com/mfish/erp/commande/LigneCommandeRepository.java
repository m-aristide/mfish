package com.mfish.erp.commande;

import com.mfish.erp.stats.TopProduit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, UUID> {

    List<LigneCommande> findByCommandeId(UUID commandeId);

    @Query("SELECT new com.mfish.erp.stats.TopProduit(p.nom, SUM(l.quantite)) "
            + "FROM LigneCommande l JOIN l.produit p JOIN l.commande c "
            + "WHERE c.dateLivraisonSouhaitee BETWEEN :debut AND :fin "
            + "AND c.statut <> com.mfish.erp.commande.StatutCommande.ANNULEE "
            + "GROUP BY p.nom ORDER BY SUM(l.quantite) DESC")
    List<TopProduit> topProduits(LocalDate debut, LocalDate fin, Pageable pageable);
}
