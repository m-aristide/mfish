package com.mfish.erp.config;

import com.mfish.erp.commande.CommandeInvalideException;
import com.mfish.erp.commande.SurpaiementException;
import com.mfish.erp.commande.TransitionStatutInvalideException;
import com.mfish.erp.commun.NotFoundException;
import com.mfish.erp.securite.IdentifiantsInvalidesException;
import com.mfish.erp.securite.TropDeTentativesException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestion globale des erreurs : reponses normalisees RFC 9457 ({@link ProblemDetail}).
 * Scope aux controleurs REST uniquement (l'interface web a son propre
 * {@code WebControllerAdvice} qui renvoie des vues HTML).
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(final NotFoundException ex) {
        return problemDetail(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ProblemDetail identifiantsInvalides(final IdentifiantsInvalidesException ex) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "Authentification requise", ex.getMessage());
    }

    @ExceptionHandler(TropDeTentativesException.class)
    public ProblemDetail tropDeTentatives(final TropDeTentativesException ex) {
        return problemDetail(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives", ex.getMessage());
    }

    @ExceptionHandler(CommandeInvalideException.class)
    public ProblemDetail commandeInvalide(final CommandeInvalideException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, "Commande invalide", ex.getMessage());
    }

    @ExceptionHandler({TransitionStatutInvalideException.class, SurpaiementException.class})
    public ProblemDetail conflit(final RuntimeException ex) {
        return problemDetail(HttpStatus.CONFLICT, "Etat incompatible", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail violationContrainte(final DataIntegrityViolationException ex) {
        return problemDetail(HttpStatus.CONFLICT, "Violation de contrainte",
                "La donnee entre en conflit avec une contrainte existante");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(final MethodArgumentNotValidException ex) {
        final ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Entree invalide",
                "Certains champs sont invalides");
        final Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                error -> erreurs.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", erreurs);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail interne(final Exception ex) {
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue");
    }

    private ProblemDetail problemDetail(final HttpStatus status, final String title, final String detail) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
