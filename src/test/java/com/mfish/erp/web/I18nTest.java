package com.mfish.erp.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class I18nTest {

    private static final Pattern CLE = Pattern.compile("#\\{([a-zA-Z0-9._]+)}");

    @Test
    @DisplayName("toutes les cles i18n utilisees dans les gabarits existent")
    void toutes_les_cles_existent() throws Exception {
        final Properties messages = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/messages.properties"))) {
            messages.load(in);
        }

        final Set<String> manquantes = new TreeSet<>();
        try (var flux = Files.walk(Path.of("src/main/resources/templates"))) {
            flux.filter(path -> path.toString().endsWith(".html")).forEach(path -> {
                final String contenu;
                try {
                    contenu = Files.readString(path);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
                final Matcher matcher = CLE.matcher(contenu);
                while (matcher.find()) {
                    final String cle = matcher.group(1);
                    if (!messages.containsKey(cle)) {
                        manquantes.add(cle);
                    }
                }
            });
        }

        assertThat(manquantes).as("cles manquantes dans messages.properties").isEmpty();
    }

    @Test
    @DisplayName("le fichier de messages contient les cles principales")
    void contient_les_cles_principales() throws Exception {
        final Properties messages = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/messages.properties"))) {
            messages.load(in);
        }
        assertThat(messages).containsKeys("app.nom", "nav.tournee", "connexion.titre",
                "commande.titre", "statut.LIVREE", "role.ADMIN");
    }
}
