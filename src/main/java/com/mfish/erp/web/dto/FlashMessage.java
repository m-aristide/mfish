package com.mfish.erp.web.dto;

/**
 * Message flash affiche apres une action (type + message deja resolu).
 */
public record FlashMessage(String type, String message) {

    public static FlashMessage succes(final String message) {
        return new FlashMessage("succes", message);
    }

    public static FlashMessage erreur(final String message) {
        return new FlashMessage("erreur", message);
    }

    public static FlashMessage info(final String message) {
        return new FlashMessage("info", message);
    }
}
