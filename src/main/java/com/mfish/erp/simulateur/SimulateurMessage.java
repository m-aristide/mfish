package com.mfish.erp.simulateur;

import java.time.Instant;

/**
 * Message sortant (reponse de l'IA) capture par le simulateur et restitue a l'interface.
 */
public record SimulateurMessage(String conversationId, String contenu, Instant horodatage) {
}
