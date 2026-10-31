package com.mfish.erp.web;

import com.mfish.erp.web.dto.FlashMessage;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Construit des messages flash a partir des cles i18n ({@code messages.properties}).
 * Le texte est resolu ici pour que les gabarits restent exempts de texte en dur.
 */
@Component
public class FlashBuilder {

    private final MessageSource messageSource;

    public FlashBuilder(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public FlashMessage succes(final String cle) {
        return FlashMessage.succes(texte(cle));
    }

    public FlashMessage erreur(final String cle) {
        return FlashMessage.erreur(texte(cle));
    }

    public FlashMessage info(final String cle) {
        return FlashMessage.info(texte(cle));
    }

    private String texte(final String cle) {
        return messageSource.getMessage(cle, null, Locale.FRENCH);
    }
}
