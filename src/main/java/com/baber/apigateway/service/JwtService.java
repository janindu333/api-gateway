package com.baber.apigateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Gateway token validation for secured routes: <b>OAuth2 / Keycloak only</b>
 * via {@code jwt.jwks-uri} (preferred) or {@code jwt.issuer-uri}. HS256 / shared-secret tokens are not accepted here.
 * <p>
 * When Keycloak is reached from pods via {@code host.docker.internal} but tokens still carry
 * {@code iss=http://localhost:9090/realms/...}, use {@code jwt.jwks-uri} plus {@code jwt.accepted-issuers}
 * (same pattern as identity-service).
 */
@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${jwt.jwks-uri:}")
    private String jwksUri;

    @Value("${jwt.accepted-issuers:}")
    private String acceptedIssuersCsv;

    private volatile JwtDecoder jwtDecoder;

    public boolean validateToken(final String token) {
        if (!isIssuerValidationEnabled()) {
            logger.warn("JWT validation skipped: neither jwt.issuer-uri nor jwt.jwks-uri is configured; refusing token");
            return false;
        }
        try {
            getOrCreateDecoder().decode(token);
            logger.debug("Token valid via OAuth2 / JWKS");
            return true;
        } catch (JwtException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isIssuerValidationEnabled() {
        return StringUtils.hasText(issuerUri) || StringUtils.hasText(jwksUri);
    }

    private JwtDecoder getOrCreateDecoder() {
        JwtDecoder local = jwtDecoder;
        if (local != null) {
            return local;
        }

        synchronized (this) {
            if (jwtDecoder != null) {
                return jwtDecoder;
            }

            // Prefer JWKS so signature is checked against the realm keys reachable from this pod,
            // while iss can still be a browser-facing localhost URL.
            if (StringUtils.hasText(jwksUri)) {
                logger.info("Using JWT JWK set validation via {}", jwksUri);
                NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri.trim()).build();
                decoder.setJwtValidator(buildJwtValidators(acceptedIssuersCsv));
                jwtDecoder = decoder;
            } else if (StringUtils.hasText(issuerUri)) {
                logger.info("Using JWT issuer validation via {}", issuerUri);
                NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri.trim());
                decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri.trim()));
                jwtDecoder = decoder;
            } else {
                throw new IllegalStateException("JWT decoder misconfiguration");
            }

            return jwtDecoder;
        }
    }

    private static OAuth2TokenValidator<Jwt> buildJwtValidators(String acceptedIssuersCsv) {
        JwtTimestampValidator timestamp = new JwtTimestampValidator();
        if (!StringUtils.hasText(acceptedIssuersCsv)) {
            return timestamp;
        }
        OAuth2TokenValidator<Jwt> issuers = jwt -> validateAcceptedIssuers(jwt, acceptedIssuersCsv);
        return new DelegatingOAuth2TokenValidator<>(timestamp, issuers);
    }

    private static OAuth2TokenValidatorResult validateAcceptedIssuers(Jwt jwt, String acceptedIssuersCsv) {
        String issRaw = jwt.getClaimAsString("iss");
        final String iss = issRaw != null ? issRaw : "";
        boolean ok = Arrays.stream(acceptedIssuersCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(allowed -> allowed.equals(iss));
        if (ok) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Unexpected iss: " + iss, null));
    }
}
