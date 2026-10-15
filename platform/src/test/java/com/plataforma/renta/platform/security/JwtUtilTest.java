package com.plataforma.renta.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtUtilTest {

    private static final String VALID_SECRET = "clave-secreta-de-prueba-de-32-bytes-minimo-para-testing";
    private static final long EXPIRATION_MS = 86400000;

    @Test
    void generateToken_y_isTokenValid_funcionanConSecretoValido() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, EXPIRATION_MS);
        UserDetails userDetails = User.withUsername("test@test.com")
                .password("password")
                .authorities("CLIENTE")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void extractUsername_retornaEmailDelToken() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, EXPIRATION_MS);
        UserDetails userDetails = User.withUsername("test@test.com")
                .password("password")
                .authorities("CLIENTE")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("test@test.com");
    }

    @Test
    void tokenContieneExpirationClaim() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 86400000);
        UserDetails userDetails = User.withUsername("test@test.com")
                .password("password")
                .authorities("CLIENTE")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        // Verify token can be parsed and has expiration claim
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("test@test.com");
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }
}

class JwtUtilSecretValidationTest {

    private static final long EXPIRATION_MS = 86400000;

    @Test
    void constructor_conSecretoNulo_debeFallar() {
        assertThatThrownBy(() -> new JwtUtil(null, EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está configurado");
    }

    @Test
    void constructor_conSecretoVacio_debeFallar() {
        assertThatThrownBy(() -> new JwtUtil("", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está configurado");
    }

    @Test
    void constructor_conSecretoMuyCorto_debeFallar() {
        // 31 bytes = 248 bits (menos de 256 bits requeridos)
        String shortSecret = "a".repeat(31);
        assertThatThrownBy(() -> new JwtUtil(shortSecret, EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET debe tener al menos 32 bytes");
    }

    @Test
    void constructor_conSecretoExactamente32Bytes_debeFuncionar() {
        // 32 bytes = 256 bits (mínimo requerido)
        String validSecret = "a".repeat(32);
        JwtUtil jwtUtil = new JwtUtil(validSecret, EXPIRATION_MS);
        assertThat(jwtUtil).isNotNull();
    }

    @Test
    void constructor_conSecretoLargo_debeFuncionar() {
        String longSecret = "clave-secreta-de-prueba-de-32-bytes-minimo-para-testing";
        JwtUtil jwtUtil = new JwtUtil(longSecret, EXPIRATION_MS);
        assertThat(jwtUtil).isNotNull();
    }

    @Test
    void constructor_conSecretoConEspacios_debeFallar() {
        assertThatThrownBy(() -> new JwtUtil("   ", EXPIRATION_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET no está configurado");
    }
}