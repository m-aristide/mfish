package com.mfish.erp.quartier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mfish.erp.commun.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuartierServiceTest {

    @Mock
    private QuartierRepository quartierRepository;

    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private QuartierService service() {
        return new QuartierService(quartierRepository);
    }

    @Test
    @DisplayName("doit_lister_les_quartiers")
    void doit_lister_les_quartiers() {
        when(quartierRepository.findAll()).thenReturn(List.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)));

        assertThat(service().lister()).hasSize(1);
    }

    @Test
    @DisplayName("doit_trouver_un_quartier")
    void doit_trouver_un_quartier() {
        when(quartierRepository.findById(id))
                .thenReturn(Optional.of(new Quartier("Cocody", "Nord", 5.35, -3.97, 1, true)));

        assertThat(service().trouver(id).nom()).isEqualTo("Cocody");
    }

    @Test
    @DisplayName("doit_creer_un_quartier")
    void doit_creer_un_quartier() {
        when(quartierRepository.save(any(Quartier.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service().creer(new QuartierRequest("Cocody", "Nord", 5.35, -3.97, 1, true)).nom())
                .isEqualTo("Cocody");
    }

    @Test
    @DisplayName("doit_lever_not_found_si_absent")
    void doit_lever_not_found_si_absent() {
        when(quartierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().trouver(id)).isInstanceOf(NotFoundException.class);
    }
}
