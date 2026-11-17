package com.mfish.erp.commande;

import com.mfish.erp.stats.CaParQuartier;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommandeRepository extends JpaRepository<Commande, UUID> {

    List<Commande> findByStatut(StatutCommande statut);

    List<Commande> findByDateLivraisonSouhaitee(LocalDate date);

    List<Commande> findByClientId(UUID clientId);

    @Query("SELECT COALESCE(SUM(c.montantTotal), 0) FROM Commande c "
            + "WHERE c.dateLivraisonSouhaitee BETWEEN :debut AND :fin "
            + "AND c.statut <> com.mfish.erp.commande.StatutCommande.ANNULEE")
    long caParPeriode(LocalDate debut, LocalDate fin);

    @Query("SELECT COUNT(c) FROM Commande c "
            + "WHERE c.dateLivraisonSouhaitee BETWEEN :debut AND :fin "
            + "AND c.statut <> com.mfish.erp.commande.StatutCommande.ANNULEE")
    long countCommandesPeriode(LocalDate debut, LocalDate fin);

    @Query("SELECT COUNT(c) FROM Commande c "
            + "WHERE c.dateLivraisonSouhaitee BETWEEN :debut AND :fin "
            + "AND c.statut = com.mfish.erp.commande.StatutCommande.LIVREE")
    long countCommandesLivrees(LocalDate debut, LocalDate fin);

    @Query("SELECT new com.mfish.erp.stats.CaParQuartier(q.nom, COALESCE(SUM(c.montantTotal), 0)) "
            + "FROM Commande c JOIN c.client cl JOIN cl.quartier q "
            + "WHERE c.dateLivraisonSouhaitee BETWEEN :debut AND :fin "
            + "AND c.statut <> com.mfish.erp.commande.StatutCommande.ANNULEE "
            + "GROUP BY q.nom ORDER BY q.nom")
    List<CaParQuartier> caParQuartier(LocalDate debut, LocalDate fin);

    @Query("SELECT COALESCE(SUM(c.montantPaye), 0) FROM Commande c")
    long sommeMontantPaye();

    @Query("SELECT COALESCE(SUM(c.montantTotal), 0) FROM Commande c "
            + "WHERE c.statut <> com.mfish.erp.commande.StatutCommande.ANNULEE")
    long sommeMontantTotalActif();

    @Query("SELECT c FROM Commande c WHERE c.statut = com.mfish.erp.commande.StatutCommande.LIVREE "
            + "AND c.statutPaiement <> com.mfish.erp.commande.StatutPaiement.PAYE")
    List<Commande> paiementsEnRetard();

    @Query("SELECT c FROM Commande c JOIN c.client cl LEFT JOIN cl.quartier q "
            + "WHERE (:statut IS NULL OR c.statut = :statut) "
            + "AND (:date IS NULL OR c.dateLivraisonSouhaitee = :date) "
            + "AND (:quartierId IS NULL OR q.id = :quartierId) "
            + "AND (:recherche IS NULL "
            + "     OR LOWER(cl.nom) LIKE :recherche "
            + "     OR LOWER(cl.telephone) LIKE :recherche) "
            + "ORDER BY c.createdAt DESC")
    Page<Commande> rechercher(StatutCommande statut, LocalDate date, UUID quartierId,
                              String recherche, Pageable pageable);
}
