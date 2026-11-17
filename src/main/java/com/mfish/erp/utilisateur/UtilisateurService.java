package com.mfish.erp.utilisateur;

import com.mfish.erp.commun.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des comptes d'acces (creation, modification, desactivation).
 * Le mot de passe n'est jamais conserve en clair : {@link PasswordEncoder}.
 */
@Service
@Transactional(readOnly = true)
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(final UtilisateurRepository utilisateurRepository,
                              final PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UtilisateurDto> lister() {
        return utilisateurRepository.findAll().stream().map(UtilisateurDto::from).toList();
    }

    public UtilisateurDto trouver(final UUID id) {
        return utilisateurRepository.findById(id).map(UtilisateurDto::from)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
    }

    @Transactional
    public UtilisateurDto creer(final String username, final String motDePasse, final Role role) {
        final Utilisateur utilisateur = new Utilisateur(username,
                passwordEncoder.encode(motDePasse), role);
        return UtilisateurDto.from(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurDto modifier(final UUID id, final Role role, final boolean actif) {
        final Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
        utilisateur.setRole(role);
        utilisateur.setActif(actif);
        return UtilisateurDto.from(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    public UtilisateurDto changerMotDePasse(final UUID id, final String nouveauMotDePasse) {
        final Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
        utilisateur.setPasswordHash(passwordEncoder.encode(nouveauMotDePasse));
        return UtilisateurDto.from(utilisateurRepository.save(utilisateur));
    }
}
