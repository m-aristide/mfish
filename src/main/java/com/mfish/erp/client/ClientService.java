package com.mfish.erp.client;

import com.mfish.erp.commun.NotFoundException;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final QuartierRepository quartierRepository;

    public ClientService(final ClientRepository clientRepository,
                         final QuartierRepository quartierRepository) {
        this.clientRepository = clientRepository;
        this.quartierRepository = quartierRepository;
    }

    public List<ClientDto> lister() {
        return clientRepository.findAll().stream()
                .filter(client -> !client.isArchive())
                .map(ClientDto::from).toList();
    }

    public Page<ClientDto> rechercher(final String recherche, final Pageable pageable) {
        final String motif = motif(recherche);
        return clientRepository.rechercherActifs(motif, pageable).map(ClientDto::from);
    }

    private static String motif(final String recherche) {
        return recherche == null || recherche.isBlank()
                ? null
                : "%" + recherche.toLowerCase() + "%";
    }

    public ClientDto trouver(final UUID id) {
        return clientRepository.findById(id).map(ClientDto::from)
                .orElseThrow(() -> new NotFoundException("Client introuvable : " + id));
    }

    @Transactional
    public ClientDto archiver(final UUID id) {
        final Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client introuvable : " + id));
        client.setArchive(true);
        return ClientDto.from(clientRepository.save(client));
    }

    @Transactional
    public ClientDto desarchiver(final UUID id) {
        final Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client introuvable : " + id));
        client.setArchive(false);
        return ClientDto.from(clientRepository.save(client));
    }

    @Transactional
    public ClientDto creer(final ClientRequest request) {
        final Client client = new Client(request.nom(), request.telephone());
        appliquer(client, request);
        return ClientDto.from(clientRepository.save(client));
    }

    @Transactional
    public ClientDto modifier(final UUID id, final ClientRequest request) {
        final Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client introuvable : " + id));
        client.setNom(request.nom());
        client.setTelephone(request.telephone());
        appliquer(client, request);
        return ClientDto.from(clientRepository.save(client));
    }

    @Transactional
    public void supprimer(final UUID id) {
        if (!clientRepository.existsById(id)) {
            throw new NotFoundException("Client introuvable : " + id);
        }
        clientRepository.deleteById(id);
    }

    private void appliquer(final Client client, final ClientRequest request) {
        final Quartier quartier = request.quartierId() == null
                ? null
                : quartierRepository.findById(request.quartierId())
                        .orElseThrow(() -> new NotFoundException("Quartier introuvable"));
        client.setQuartier(quartier);
        client.setAdresse(request.adresse());
        client.setRepere(request.repere());
        client.setLatitude(request.latitude());
        client.setLongitude(request.longitude());
        client.setNotes(request.notes());
    }
}
