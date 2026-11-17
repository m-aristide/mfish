package com.mfish.erp.utilisateur;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    Optional<Utilisateur> findByUsername(String username);

    boolean existsByUsername(String username);
}
