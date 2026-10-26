package com.hssc.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder passwords; private final JwtEncoder encoder;
    private final String issuer; private final long expirationMinutes;
    public AuthService(UserRepository users,PasswordEncoder passwords,JwtEncoder encoder,
            @Value("${hssc.jwt.issuer}") String issuer,@Value("${hssc.jwt.expiration-minutes}") long expirationMinutes){
        this.users=users;this.passwords=passwords;this.encoder=encoder;this.issuer=issuer;this.expirationMinutes=expirationMinutes;
    }
    public Api.TokenResponse login(Api.LoginRequest request){
        UserAccount user=users.findByEmailIgnoreCase(request.email()).orElseThrow(()->ApiException.forbidden("Invalid email or password"));
        if(!passwords.matches(request.password(),user.getPasswordHash())) throw ApiException.forbidden("Invalid email or password");
        if(user.getStatus()==AccountStatus.PENDING) {
            String message=user.getRole()==Role.SCHOOL
                    ? "Your account is still under review. Please wait for administrator approval before signing in."
                    : "Your account is still under review. Please wait for approval before signing in.";
            throw ApiException.forbidden(message);
        }
        if(user.getStatus()!=AccountStatus.ACTIVE) throw ApiException.forbidden("This account is disabled or has not been approved.");
        Instant now=Instant.now(), expires=now.plus(Duration.ofMinutes(expirationMinutes));
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer(issuer).issuedAt(now).expiresAt(expires).subject(user.getId().toString())
                .claim("roles",List.of(user.getRole().name())).claim("email",user.getEmail()).build();
        JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();
        String token=encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
        return new Api.TokenResponse(token,"Bearer",expires,user.getId(),user.getRole().name(),user.getStatus().name());
    }
}
