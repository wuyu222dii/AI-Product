package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.config.SecurityConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = AuthenticatedApiTest.Factory.class)
@Import(SecurityConfig.class)
public @interface AuthenticatedApiTest {

    String subject() default "11111111-1111-1111-1111-111111111111";

    class Factory implements WithSecurityContextFactory<AuthenticatedApiTest> {
        @Override
        public SecurityContext createSecurityContext(AuthenticatedApiTest annotation) {
            Instant now = Instant.now();
            Jwt jwt = Jwt.withTokenValue("controller-test-token")
                    .header("alg", "none")
                    .subject(annotation.subject())
                    .audience(List.of("authenticated"))
                    .claim("email", "controller-test@example.edu")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300))
                    .build();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"))
            ));
            return context;
        }
    }
}
