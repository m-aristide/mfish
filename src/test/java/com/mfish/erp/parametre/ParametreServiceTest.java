package com.mfish.erp.parametre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParametreServiceTest {

    @Mock
    private ParametreRepository parametreRepository;

    private ParametreService service() {
        return new ParametreService(parametreRepository);
    }

    @Test
    @DisplayName("doit_lister_les_parametres")
    void doit_lister_les_parametres() {
        when(parametreRepository.findAll()).thenReturn(List.of(new Parametre("cle", "valeur")));

        assertThat(service().lister()).hasSize(1);
    }

    @Test
    @DisplayName("doit_enregistrer_un_nouveau_parametre")
    void doit_enregistrer_un_nouveau_parametre() {
        when(parametreRepository.findByCle("cle")).thenReturn(Optional.empty());
        when(parametreRepository.save(any(Parametre.class))).thenAnswer(inv -> inv.getArgument(0));

        final ParametreDto resultat = service().enregistrer("cle", "valeur");

        assertThat(resultat.valeur()).isEqualTo("valeur");
    }

    @Test
    @DisplayName("doit_mettre_a_jour_un_parametre_existant")
    void doit_mettre_a_jour_un_parametre_existant() {
        final Parametre existant = new Parametre("cle", "ancienne");
        when(parametreRepository.findByCle("cle")).thenReturn(Optional.of(existant));
        when(parametreRepository.save(any(Parametre.class))).thenAnswer(inv -> inv.getArgument(0));

        final ParametreDto resultat = service().enregistrer("cle", "nouvelle");

        assertThat(resultat.valeur()).isEqualTo("nouvelle");
    }

    @Test
    @DisplayName("doit_retourner_toutes_les_valeurs")
    void doit_retourner_toutes_les_valeurs() {
        when(parametreRepository.findAll()).thenReturn(List.of(new Parametre("a", "1")));

        assertThat(service().toutesLesValeurs()).containsEntry("a", "1");
    }
}
