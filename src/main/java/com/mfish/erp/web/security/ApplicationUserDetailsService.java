package com.mfish.erp.web.security;

import com.mfish.erp.securite.LoginRateLimiter;
import com.mfish.erp.utilisateur.Utilisateur;
import com.mfish.erp.utilisateur.UtilisateurRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Charge les comptes d'acces depuis la base pour l'authentification par formulaire.
 * Applique aussi la limitation de tentatives (verrouillage temporaire apres 5 echecs).
 */
@Service
public class ApplicationUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final LoginRateLimiter loginRateLimiter;

    public ApplicationUserDetailsService(final UtilisateurRepository utilisateurRepository,
                                         final LoginRateLimiter loginRateLimiter) {
        this.utilisateurRepository = utilisateurRepository;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        if (!loginRateLimiter.autorise(username)) {
            throw new LockedException("Trop de tentatives de connexion");
        }
        final Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
        if (!utilisateur.isActif()) {
            throw new DisabledException("Compte desactive");
        }
        return User.withUsername(utilisateur.getUsername())
                .password(utilisateur.getPasswordHash())
                .roles(utilisateur.getRole().name())
                .build();
    }
}
