package com.hssc.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

@Configuration
public class BootstrapConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean CommandLineRunner bootstrap(UserRepository users,DistrictSeedService districtSeed,
            PasswordEncoder passwords,@Value("${hssc.bootstrap.admin-email}") String adminEmail,
            @Value("${hssc.bootstrap.admin-password}") String adminPassword,
            @Value("${hssc.bootstrap.district-zones}") String districtZoneList) {
        return args -> {
            if(adminEmail.isBlank()!=adminPassword.isBlank()) throw new IllegalStateException("Set both HSSC_ADMIN_EMAIL and HSSC_ADMIN_PASSWORD, or neither");
            if(!adminEmail.isBlank()){
                if(adminPassword.length()<16) throw new IllegalStateException("HSSC_ADMIN_PASSWORD must contain at least 16 characters");
                Optional<UserAccount> existing=users.findByEmailIgnoreCase(adminEmail);
                if(existing.isEmpty())
                    users.save(new UserAccount(adminEmail,passwords.encode(adminPassword),Role.ADMIN,AccountStatus.ACTIVE));
                else if(existing.get().getRole()!=Role.ADMIN)
                    throw new IllegalStateException("HSSC_ADMIN_EMAIL is already registered to a non-administrator account");
            }
            districtSeed.seed(districtZoneList);
        };
    }
}
