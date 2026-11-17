package com.mfish.erp.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplateSecurityTest {

    @Test
    @DisplayName("aucun template n'utilise th:utext (echappement par defaut)")
    void aucun_th_utext() throws Exception {
        try (var flux = Files.walk(Path.of("src/main/resources/templates"))) {
            final var fichiers = flux.filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("th:utext");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .map(Path::toString)
                    .toList();
            assertThat(fichiers).as("fichiers contenant th:utext").isEmpty();
        }
    }

    @Test
    @DisplayName("aucun script inline (CSP stricte)")
    void aucun_script_inline() throws Exception {
        try (var flux = Files.walk(Path.of("src/main/resources/templates"))) {
            final var fichiers = flux.filter(path -> path.toString().endsWith(".html"))
                    .filter(path -> {
                        try {
                            final String contenu = Files.readString(path);
                            return contenu.contains("<script")
                                    && !contenu.contains("th:src=")
                                    && !contenu.contains("src=");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .map(Path::toString)
                    .toList();
            assertThat(fichiers).as("fichiers contenant un script inline").isEmpty();
        }
    }
}
