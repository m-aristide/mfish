package com.mfish.erp.whatsapp.client;

/**
 * Media entrant telecharge depuis l'API Meta (contenu binaire + type MIME).
 */
public record MediaTelecharge(byte[] contenu, String typeMime) {
}
