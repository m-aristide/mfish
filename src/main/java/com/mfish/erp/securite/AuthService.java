package com.mfish.erp.securite;

import com.mfish.erp.config.AdminProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentification de l'administrateur et emission du jeton JWT.
 */
@Service
public class AuthService {

    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(final AdminProperties adminProperties,
                       final PasswordEncoder passwordEncoder,
                       final JwtService jwtService) {
        this.adminProperties = adminProperties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Verifie les identifiants et retourne un jeton JWT.
     */
    public TokenResponse login(final LoginRequest request) {
        final boolean utilisateurOk = adminProperties.username().equals(request.username());
        final boolean motDePasseOk = passwordEncoder.matches(
                request.password(), adminProperties.passwordHash());
        if (!utilisateurOk || !motDePasseOk) {
            throw new IdentifiantsInvalidesException();
        }
        return new TokenResponse(jwtService.generer(request.username()));
    }
}
