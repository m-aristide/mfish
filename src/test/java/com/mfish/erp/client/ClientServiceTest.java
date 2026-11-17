package com.mfish.erp.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mfish.erp.commun.NotFoundException;
import com.mfish.erp.quartier.Quartier;
import com.mfish.erp.quartier.QuartierRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private QuartierRepository quartierRepository;

    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID quartierId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private ClientService service() {
        return new ClientService(clientRepository, quartierRepository);
    }

    @Test
    @DisplayName("doit_lister_les_clients")
    void doit_lister_les_clients() {
        when(clientRepository.findAll()).thenReturn(List.of(new Client("Awa", "+2250700000001")));

        assertThat(service().lister()).hasSize(1);
    }

    @Test
    @DisplayName("doit_creer_un_client_avec_quartier")
    void doit_creer_un_client_avec_quartier() {
        final Quartier quartier = mock(Quartier.class);
        when(quartier.getId()).thenReturn(quartierId);
        when(quartierRepository.findById(quartierId)).thenReturn(Optional.of(quartier));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        final ClientDto resultat = service().creer(new ClientRequest("Awa", "+2250700000001",
                quartierId, null, null, null, null, null));

        assertThat(resultat.nom()).isEqualTo("Awa");
        assertThat(resultat.quartierId()).isEqualTo(quartierId);
    }

    @Test
    @DisplayName("doit_creer_un_client_sans_quartier")
    void doit_creer_un_client_sans_quartier() {
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        final ClientDto resultat = service().creer(new ClientRequest("Awa", "+2250700000001",
                null, null, null, null, null, null));

        assertThat(resultat.quartierId()).isNull();
    }

    @Test
    @DisplayName("doit_lever_not_found_si_absent")
    void doit_lever_not_found_si_absent() {
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().trouver(id)).isInstanceOf(NotFoundException.class);
    }
}
