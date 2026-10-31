package com.mfish.erp.produit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduitRepository extends JpaRepository<Produit, UUID> {

    Optional<Produit> findByNomIgnoreCase(String nom);

    List<Produit> findByActifTrueOrderByNomAsc();
}
