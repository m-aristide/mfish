package com.mfish.erp.quartier;

import com.mfish.erp.commun.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QuartierService {

    private final QuartierRepository quartierRepository;

    public QuartierService(final QuartierRepository quartierRepository) {
        this.quartierRepository = quartierRepository;
    }

    public List<QuartierDto> lister() {
        return quartierRepository.findAll().stream().map(QuartierDto::from).toList();
    }

    public QuartierDto trouver(final UUID id) {
        return quartierRepository.findById(id).map(QuartierDto::from)
                .orElseThrow(() -> new NotFoundException("Quartier introuvable : " + id));
    }

    public QuartierDto creer(final QuartierRequest request) {
        final Quartier quartier = new Quartier(request.nom(), request.zone(), request.latitude(),
                request.longitude(), request.ordreDefaut(), request.actif());
        return QuartierDto.from(quartierRepository.save(quartier));
    }

    /**
     * Indique si un autre quartier porte deja ce nom (comparaison sans tenir compte
     * de la casse). Utile aux formulaires pour refuser un doublon avec un message clair
     * plutot qu'une violation de contrainte d'unicite.
     *
     * @param nom nom candidat (non nul)
     * @param idExclu identifiant du quartier en cours de modification, ou {@code null}
     *     lors d'une creation
     */
    public boolean nomDejaUtilise(final String nom, final UUID idExclu) {
        Objects.requireNonNull(nom, "nom");
        return quartierRepository.findByNomIgnoreCase(nom)
                .filter(quartier -> idExclu == null || !idExclu.equals(quartier.getId()))
                .isPresent();
    }

    public QuartierDto modifier(final UUID id, final QuartierRequest request) {
        final Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quartier introuvable : " + id));
        quartier.setNom(request.nom());
        quartier.setZone(request.zone());
        quartier.setLatitude(request.latitude());
        quartier.setLongitude(request.longitude());
        quartier.setOrdreDefaut(request.ordreDefaut());
        quartier.setActif(request.actif());
        return QuartierDto.from(quartierRepository.save(quartier));
    }

    public void supprimer(final UUID id) {
        if (!quartierRepository.existsById(id)) {
            throw new NotFoundException("Quartier introuvable : " + id);
        }
        quartierRepository.deleteById(id);
    }
}
