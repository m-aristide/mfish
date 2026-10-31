package com.mfish.erp.ia;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Analyse la sortie JSON stricte de l'IA en {@link ReponseIa}. Les champs inconnus sont ignores.
 */
@Component
public class ParseurReponseIa {

    private final ObjectMapper objectMapper;

    public ParseurReponseIa() {
        this.objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public ReponseIa parser(final String json) {
        try {
            return objectMapper.readValue(json, ReponseIa.class);
        } catch (JacksonException e) {
            throw new ReponseIaInvalideException("Sortie IA invalide", e);
        }
    }
}
