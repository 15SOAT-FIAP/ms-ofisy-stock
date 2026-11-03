package br.com.ofisy.ms_ofisy_stock.shared.jwt;

import br.com.ofisy.ms_ofisy_stock.config.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = JwtTestTokens.SECRET;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        jwtService = new JwtService(properties);
    }

    @Nested
    class IsValidToken {

        @Test
        void shouldAcceptTokenIssuedByTheCore() {
            assertThat(jwtService.isValidToken(JwtTestTokens.withRole(SECRET, "STOCKMAN"))).isTrue();
        }

        @Test
        void shouldRejectTokenSignedWithAnotherSecret() {
            assertThat(jwtService.isValidToken(JwtTestTokens.withRole(JwtTestTokens.OTHER_SECRET, "STOCKMAN"))).isFalse();
        }

        @Test
        void shouldRejectExpiredToken() {
            assertThat(jwtService.isValidToken(JwtTestTokens.expired(SECRET, "STOCKMAN"))).isFalse();
        }

        @Test
        void shouldRejectTokenFromAnotherIssuer() {
            String customerToken = JwtTestTokens.withIssuer(SECRET, JwtTestTokens.CUSTOMER_ISSUER, "STOCKMAN");

            assertThat(jwtService.isValidToken(customerToken)).isFalse();
        }

        @Test
        void shouldRejectMalformedToken() {
            assertThat(jwtService.isValidToken("not-a-jwt")).isFalse();
        }
    }

    @Nested
    class ExtractClaims {

        @Test
        void shouldExtractEmailAndRole() {
            String token = JwtTestTokens.withRole(SECRET, "MECHANIC");

            assertThat(jwtService.extractEmail(token)).isEqualTo(JwtTestTokens.EMAIL);
            assertThat(jwtService.extractRole(token)).isEqualTo("MECHANIC");
        }

        @Test
        void shouldReturnNullRoleWhenClaimIsMissing() {
            assertThat(jwtService.extractRole(JwtTestTokens.withoutRole(SECRET))).isNull();
        }
    }
}
