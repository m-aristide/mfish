package com.mfish.erp.securite;

import com.mfish.erp.config.JwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Emission et validation de jetons JWT signes en HMAC-SHA256 (Nimbus JOSE JWT 10).
 */
@Service
public class JwtService {

    private final byte[] secret;
    private final Duration validity;
    private final Clock clock;

    public JwtService(final JwtProperties properties, final Clock clock) {
        this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("APP_JWT_SECRET doit faire au moins 32 octets");
        }
        this.validity = properties.validity();
        this.clock = clock;
    }

    /**
     * Emet un jeton pour le sujet donne.
     */
    public String generer(final String sujet) {
        final Instant now = clock.instant();
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sujet)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(validity)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        final SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Impossible de signer le jeton JWT", e);
        }
    }

    /**
     * Valide un jeton et retourne son sujet, ou {@code empty} si le jeton est invalide,
     * mal signe ou expire.
     */
    public Optional<String> valider(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            final SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            final JWTClaimsSet claims = jwt.getJWTClaimsSet();
            final Date expiration = claims.getExpirationTime();
            if (expiration == null || clock.instant().isAfter(expiration.toInstant())) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (ParseException | JOSEException e) {
            // Jeton invalide, malforme ou mal signe : considere comme non authentifie.
            return Optional.empty();
        }
    }
}
