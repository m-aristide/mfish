package com.mfish.erp.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Redirige vers la page de connexion avec un parametre distinct selon l'erreur
 * (identifiants invalides ou compte temporairement verrouille).
 */
@Component
public class WebAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final AuthenticationException exception)
            throws IOException, ServletException {
        final String parametre = exception instanceof LockedException ? "verrouille" : "erreur";
        response.sendRedirect(request.getContextPath() + "/connexion?" + parametre);
    }
}
