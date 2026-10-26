package com.hssc.backend;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth){this.auth=auth;}
    @PostMapping("/login") public Api.TokenResponse login(@Valid @RequestBody Api.LoginRequest request){return auth.login(request);}
}
