package com.mfish.erp.quartier;

import com.mfish.erp.commun.NotFoundException;
import java.util.List;
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
