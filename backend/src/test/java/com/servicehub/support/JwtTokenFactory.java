package com.servicehub.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Single source of truth for JWT tokens in controller slice tests.
 *
 * All @WebMvcTest classes reference JWT_SECRET in @TestPropertySource so
 * the Spring Security filter and this factory use the same signing key:
 *
 *   @TestPropertySource(properties = "jwt.secret=" + JwtTokenFactory.JWT_SECRET)
 */
public final class JwtTokenFactory {

    public static final String JWT_SECRET =
            "test-secret-key-for-testing-purposes-only-minimum-32-chars";

    private JwtTokenFactory() {}

    public static String managerToken()  { return token("mgr@test.com",  "MANAGER");  }
    public static String agentToken()    { return token("agt@test.com",  "AGENT");    }
    public static String employeeToken() { return token("emp@test.com",  "EMPLOYEE"); }

    public static String token(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(email).claim("role", role).signWith(key).compact();
    }
}
