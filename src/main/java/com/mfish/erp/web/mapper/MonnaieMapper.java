package com.mfish.erp.web.mapper;

import com.mfish.erp.web.config.MonnaieProperties;
import org.springframework.stereotype.Component;

/**
 * Formate les montants entiers (plus petite unite monetaire) pour l'affichage :
 * {@code 1500} devient {@code 1 500 FCFA} (groupes de 3 chiffres, sans decimale).
 */
@Component
public class MonnaieMapper {

    private final MonnaieProperties properties;

    public MonnaieMapper(final MonnaieProperties properties) {
        this.properties = properties;
    }

    public String formater(final long montant) {
        return grouper(montant) + " " + properties.devise();
    }

    private static String grouper(final long montant) {
        final String chiffres = Long.toString(montant);
        final StringBuilder resultat = new StringBuilder();
        int compte = 0;
        for (int i = chiffres.length() - 1; i >= 0; i--) {
            resultat.append(chiffres.charAt(i));
            compte++;
            if (compte % 3 == 0 && i > 0) {
                resultat.append(' ');
            }
        }
        return resultat.reverse().toString();
    }
}
