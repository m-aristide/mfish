package com.mfish.erp.connaissance;

import com.mfish.erp.commun.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de connaissance de l'IA (Markdown) avec historique de versions et retour arriere.
 * Chaque enregistrement cree une nouvelle version ; la version courante est la plus recente.
 */
@Service
@Transactional(readOnly = true)
public class ConnaissanceService {

    private static final String RESSOURCE_PAR_DEFAUT = "/connaissance/base-connaissance.md";

    private static final String CONTENU_PAR_DEFAUT = chargerContenuParDefaut();

    private final ConnaissanceRepository connaissanceRepository;

    public ConnaissanceService(final ConnaissanceRepository connaissanceRepository) {
        this.connaissanceRepository = connaissanceRepository;
    }

    public String contenuCourant() {
        return connaissanceRepository.findTopByOrderByCreatedAtDesc()
                .map(Connaissance::getContenu)
                .orElse(CONTENU_PAR_DEFAUT);
    }

    private static String chargerContenuParDefaut() {
        try (InputStream entree = ConnaissanceService.class.getResourceAsStream(RESSOURCE_PAR_DEFAUT)) {
            if (entree == null) {
                throw new IllegalStateException("Ressource introuvable : " + RESSOURCE_PAR_DEFAUT);
            }
            return new String(entree.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de lire la base de connaissance par defaut", e);
        }
    }

    public List<ConnaissanceDto> versions() {
        return connaissanceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ConnaissanceDto::from)
                .toList();
    }

    @Transactional
    public ConnaissanceDto enregistrer(final String contenu) {
        return ConnaissanceDto.from(connaissanceRepository.save(new Connaissance(contenu)));
    }

    @Transactional
    public ConnaissanceDto restaurer(final UUID id) {
        final Connaissance version = connaissanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Version introuvable : " + id));
        return ConnaissanceDto.from(connaissanceRepository.save(new Connaissance(version.getContenu())));
    }

    /**
     * Representation publique d'une version de la base de connaissance.
     */
    public record ConnaissanceDto(UUID id, Instant createdAt, int longueur) {

        static ConnaissanceDto from(final Connaissance connaissance) {
            return new ConnaissanceDto(connaissance.getId(), connaissance.getCreatedAt(),
                    connaissance.getContenu() == null ? 0 : connaissance.getContenu().length());
        }
    }
}
