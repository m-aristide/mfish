package com.mfish.erp.web.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controleur web de base : helpers communs (detection htmx, choix vue/page vs fragment).
 */
public abstract class AbstractWebController {

    protected static final String ENTETE_HX = "HX-Request";

    protected boolean estHtmx(final HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader(ENTETE_HX));
    }

    /**
     * Renvoie le fragment si la requete est htmx, sinon la page complete (meme point d'entree).
     */
    protected String vue(final HttpServletRequest request, final String page, final String fragment) {
        return estHtmx(request) ? fragment : page;
    }
}
