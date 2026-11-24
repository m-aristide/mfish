package com.mfish.erp.tournee;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArretTourneeRepository extends JpaRepository<ArretTournee, UUID> {

    /**
     * Charge les arrets d'une tournee avec leur commande et le client de celle-ci.
     * L'interface web etant rendue hors transaction ({@code open-in-view=false}), ces
     * associations doivent etre initialisees pendant la requete pour eviter une
     * {@code LazyInitializationException} lors de l'assemblage de la vue.
     */
    @EntityGraph(attributePaths = {"commande", "commande.client"})
    List<ArretTournee> findByTourneeIdOrderByPositionAsc(UUID tourneeId);

    void deleteAllByTourneeId(UUID tourneeId);
}
