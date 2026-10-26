package com.hssc.backend;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean CorsConfigurationSource corsConfigurationSource(
            @Value("${hssc.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean SecretKey jwtKey(@Value("${hssc.jwt.secret}") String secret) {
        if(secret==null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured with a base64-encoded 256-bit secret");
        try {
            byte[] decoded=Base64.getDecoder().decode(secret);
            if(decoded.length<32) throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
            return new SecretKeySpec(decoded,"HmacSHA256");
        } catch(IllegalArgumentException ex){throw new IllegalStateException("JWT_SECRET must be valid base64",ex);}
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key){return new NimbusJwtEncoder(new ImmutableSecret<>(key));}
    @Bean JwtDecoder jwtDecoder(SecretKey key,@Value("${hssc.jwt.issuer}") String issuer){
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));return decoder;
    }
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter(){
        return jwt -> {
            List<String> roles=jwt.getClaimAsStringList("roles");
            List<SimpleGrantedAuthority> authorities=(roles==null?List.<String>of():roles).stream()
                    .map(role->new SimpleGrantedAuthority("ROLE_"+role)).toList();
            return new JwtAuthenticationToken(jwt,authorities,jwt.getSubject());
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http, UserRepository users) throws Exception {
        return http.cors(Customizer.withDefaults()).csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/api/registrations/**","/api/schools/register","/api/schools/register/with-students","/api/students/register",
                        "/api/schools","/api/sponsors","/api/policy",
                        "/api/districts","/api/exams/start","/api/exams/*/attempts","/api/exams/attempts/**","/api/exams/*/time",
                        "/api/exams/*/events","/actuator/health/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,"/api/exams/*/answers/*").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/exams/*/submit").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/exams/*/events").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/exams/**").hasAnyRole("ADMIN","SCHOOL","STUDENT")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwtAuthConverter())))
                .addFilterAfter(new AccountStatusFilter(users),BearerTokenAuthenticationFilter.class).build();
    }
}
