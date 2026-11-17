package com.mfish.erp.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mfish.erp.config.AdminProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService() {
        return new AuthService(new AdminProperties("admin", "{noop}test"), passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("doit_retourner_un_jeton_pour_de_bons_identifiants")
    void doit_retourner_un_jeton_pour_de_bons_identifiants() {
        when(passwordEncoder.matches("test", "{noop}test")).thenReturn(true);
        when(jwtService.generer("admin")).thenReturn("jeton");

        final TokenResponse response = authService().login(new LoginRequest("admin", "test"));

        assertThat(response.token()).isEqualTo("jeton");
    }

    @Test
    @DisplayName("doit_refuser_de_mauvais_identifiants")
    void doit_refuser_de_mauvais_identifiants() {
        when(passwordEncoder.matches("mauvais", "{noop}test")).thenReturn(false);

        assertThatThrownBy(() -> authService().login(new LoginRequest("admin", "mauvais")))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }

    @Test
    @DisplayName("doit_refuser_un_utilisateur_inconnu")
    void doit_refuser_un_utilisateur_inconnu() {
        assertThatThrownBy(() -> authService().login(new LoginRequest("inconnu", "test")))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }
}
