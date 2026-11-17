package com.mfish.erp.connaissance;

import com.mfish.erp.ia.LlmPort;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Zone de test de l'IA : repond a une question client fictive a partir de la base de
 * connaissance, sans aucun envoi WhatsApp.
 */
@Service
public class TestIaService {

    private final LlmPort llmPort;
    private final ConnaissanceService connaissanceService;

    public TestIaService(final LlmPort llmPort, final ConnaissanceService connaissanceService) {
        this.llmPort = llmPort;
        this.connaissanceService = connaissanceService;
    }

    public String tester(final String question) {
        final String contenu = connaissanceService.contenuCourant();
        final String prompt = "Tu es l'assistant d'un petit commercant. Utilise uniquement la "
                + "base de connaissance suivante pour repondre, de facon breve et en francais.\n\n"
                + "Base de connaissance :\n" + contenu;
        return llmPort.generer(prompt, List.of(question));
    }
}
