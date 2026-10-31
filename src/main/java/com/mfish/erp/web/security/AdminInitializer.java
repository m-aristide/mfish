package com.mfish.erp.web.security;

import com.mfish.erp.config.AdminProperties;
import com.mfish.erp.utilisateur.Role;
import com.mfish.erp.utilisateur.Utilisateur;
import com.mfish.erp.utilisateur.UtilisateurRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Cree/synchronise le compte administrateur initial depuis {@code app.admin.*}
 * (username + password-hash injectes par variables d'environnement, jamais en dur).
 * Implemente {@link InitializingBean} pour etre execute aussi pendant les tests.
 */
@Component
public class AdminInitializer implements InitializingBean {

    private final UtilisateurRepository utilisateurRepository;
    private final AdminProperties adminProperties;

    public AdminInitializer(final UtilisateurRepository utilisateurRepository,
                            final AdminProperties adminProperties) {
        this.utilisateurRepository = utilisateurRepository;
        this.adminProperties = adminProperties;
    }

    @Override
    public void afterPropertiesSet() {
        final Utilisateur admin = utilisateurRepository.findByUsername(adminProperties.username())
                .orElseGet(() -> new Utilisateur(adminProperties.username(),
                        adminProperties.passwordHash(), Role.ADMIN));
        admin.setUsername(adminProperties.username());
        admin.setPasswordHash(adminProperties.passwordHash());
        admin.setRole(Role.ADMIN);
        admin.setActif(true);
        utilisateurRepository.save(admin);
    }
}

