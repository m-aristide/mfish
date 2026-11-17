package com.mfish.erp.stockage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stockage local des fichiers televerses (images de produits). Les fichiers sont
 * ecrits sous le repertoire {@code app.stockage.uploads-dir} (defaut {@code uploads})
 * et exposes par l'URL relative {@code /uploads/...}.
 */
@Service
public class StockageService {

    private static final Logger LOG = LoggerFactory.getLogger(StockageService.class);

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/svg+xml", ".svg",
            "image/gif", ".gif");

    private final Path racine;

    public StockageService(@Value("${app.stockage.uploads-dir:uploads}") final String uploadsDir) {
        this.racine = Path.of(uploadsDir).toAbsolutePath().normalize();
    }

    /**
     * Enregistre le fichier sous {@code racine/sousDossier/} et retourne son chemin
     * d'acces relatif (ex. {@code /uploads/produits/<uuid>.png}).
     */
    public String stocker(final MultipartFile fichier, final String sousDossier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new StockageException("Le fichier est vide.");
        }
        final String extension = EXTENSIONS.get(fichier.getContentType());
        if (extension == null) {
            throw new StockageException("Type d'image non autorise.");
        }
        final Path dossier = racine.resolve(sousDossier);
        try {
            Files.createDirectories(dossier);
            final String nom = UUID.randomUUID() + extension;
            final Path cible = dossier.resolve(nom);
            try (var entree = fichier.getInputStream()) {
                Files.copy(entree, cible, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + sousDossier + "/" + nom;
        } catch (IOException exception) {
            throw new StockageException("Impossible d'enregistrer l'image.", exception);
        }
    }

    /**
     * Supprime le fichier correspondant a un chemin {@code /uploads/...} si celui-ci
     * existe. Ignore (avec journalisation) toute erreur non critique de suppression.
     */
    public void supprimer(final String chemin) {
        if (chemin == null || !chemin.startsWith("/uploads/")) {
            return;
        }
        final String relatif = chemin.substring("/uploads/".length());
        final Path cible = racine.resolve(relatif).normalize();
        if (!cible.startsWith(racine)) {
            return;
        }
        try {
            Files.deleteIfExists(cible);
        } catch (IOException exception) {
            LOG.warn("Suppression du fichier impossible : {}", cible, exception);
        }
    }
}