package com.mfish.erp.produit;

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
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ProduitService service() {
        return new ProduitService(produitRepository);
    }

    @Test
    @DisplayName("doit_lister_les_produits")
    void doit_lister_les_produits() {
        when(produitRepository.findAll()).thenReturn(List.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")));

        assertThat(service().lister()).hasSize(1);
    }

    @Test
    @DisplayName("doit_trouver_un_produit")
    void doit_trouver_un_produit() {
        when(produitRepository.findById(id)).thenReturn(Optional.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")));

        assertThat(service().trouver(id).nom()).isEqualTo("Attieke");
    }

    @Test
    @DisplayName("doit_creer_un_produit")
    void doit_creer_un_produit() {
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service().creer(new ProduitRequest("Attieke", 1000, "plat", true, "/images/p.png")).nom())
                .isEqualTo("Attieke");
    }

    @Test
    @DisplayName("doit_modifier_un_produit")
    void doit_modifier_un_produit() {
        when(produitRepository.findById(id)).thenReturn(Optional.of(new Produit("Attieke", 1000, "plat", true, "/images/default-produit.svg")));
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service().modifier(id, new ProduitRequest("Attieke2", 1200, "plat", true, "/images/p.png")).nom())
                .isEqualTo("Attieke2");
    }

    @Test
    @DisplayName("doit_supprimer_un_produit")
    void doit_supprimer_un_produit() {
        when(produitRepository.existsById(id)).thenReturn(true);

        service().supprimer(id);
    }

    @Test
    @DisplayName("doit_lever_not_found_si_absent")
    void doit_lever_not_found_si_absent() {
        when(produitRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().trouver(id)).isInstanceOf(NotFoundException.class);
    }
}
