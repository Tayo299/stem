package com.hssc.backend;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CurrentAccount {
    private final UserRepository users;
    public CurrentAccount(UserRepository users){this.users=users;}
    public UserAccount get(Jwt jwt){
        UserAccount user=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow(()->ApiException.notFound("Account not found"));
        requireActive(user);return user;
    }
    public void requireActive(UserAccount user){
        if(user.getStatus()!=AccountStatus.ACTIVE) throw ApiException.forbidden("Account is not active");
    }
}
