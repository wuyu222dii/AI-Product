package com.aipm.cowriting.config;

import com.aipm.cowriting.common.api.ApiError;
import com.aipm.cowriting.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                "UNAUTHORIZED", "登录已失效，请重新登录"
                        )))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) ->
                        writeSecurityError(
                                response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                "FORBIDDEN", "无权执行此操作"
                        )));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.jwt.audience:authenticated}") String audience
    ) {
        String baseUrl = supabaseUrl.replaceAll("/+$", "");
        // Supabase signing keys are ES256 (EC P-256). Spring's default JWS allow-list is RS256-only,
        // which rejects every valid Supabase access token and surfaces as "登录已失效".
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                baseUrl + "/auth/v1/.well-known/jwks.json"
        )
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();

        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(baseUrl + "/auth/v1");
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            Object rawAudience = jwt.getClaim("aud");
            if (audience.equals(rawAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        };
        OAuth2TokenValidator<Jwt> subjectValidator = jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid subject", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audienceValidator, subjectValidator));
        return decoder;
    }

    private void writeSecurityError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String code,
            String message
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                ApiError.of(code, message),
                Map.of("requestId", UUID.randomUUID().toString())
        ));
    }
}
