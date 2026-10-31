package com.mfish.erp.web.advice;

import com.mfish.erp.commun.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Attributs globaux et gestion des erreurs pour les controleurs web (rendu serveur).
 * Ne s'applique qu'aux controleurs du package {@code com.mfish.erp.web.controller}.
 */
@ControllerAdvice("com.mfish.erp.web.controller")
public class WebControllerAdvice {

    /**
     * Expose au gabarit si l'utilisateur courant possede le role ADMIN (pour masquer
     * les entrees de navigation reservees).
     */
    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * Expose le nom de l'utilisateur courant (null si anonyme).
     */
    @ModelAttribute("utilisateurCourant")
    public String utilisateurCourant() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Expose le jeton CSRF au gabarit (pour le meta du layout et les formulaires).
     */
    @ModelAttribute("csrf")
    public CsrfToken csrf(final HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = request.getAttribute("_csrf");
        }
        return token instanceof CsrfToken csrfToken ? csrfToken : null;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(final NotFoundException exception, final Model model) {
        model.addAttribute("message", exception.getMessage());
        return "erreurs/404";
    }
}
