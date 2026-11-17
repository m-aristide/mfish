package com.mfish.erp.whatsapp.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoupe un message depassant la limite de longueur WhatsApp en morceaux,
 * en privilegiant les coupures sur des espaces pour ne pas tronquer un mot.
 */
public final class DecoupeurMessage {

    private DecoupeurMessage() {
    }

    public static List<String> decouper(final String texte, final int limite) {
        if (texte == null || texte.isEmpty()) {
            return List.of("");
        }
        if (texte.length() <= limite) {
            return List.of(texte);
        }
        final List<String> morceaux = new ArrayList<>();
        int debut = 0;
        while (debut < texte.length()) {
            int fin = Math.min(debut + limite, texte.length());
            if (fin < texte.length()) {
                final int espace = texte.lastIndexOf(' ', fin);
                if (espace > debut) {
                    fin = espace;
                }
            }
            morceaux.add(texte.substring(debut, fin).trim());
            debut = fin;
            while (debut < texte.length() && texte.charAt(debut) == ' ') {
                debut++;
            }
        }
        return morceaux;
    }
}
