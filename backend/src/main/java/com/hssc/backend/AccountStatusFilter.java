package com.hssc.backend;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public AccountStatusFilter(UserRepository users){this.users=users;}
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        Authentication auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(auth instanceof JwtAuthenticationToken jwtAuth){
            try{
                UserAccount user=users.findById(UUID.fromString(jwtAuth.getToken().getSubject())).orElse(null);
                List<String> roles=jwtAuth.getToken().getClaimAsStringList("roles");
                if(user==null || user.getStatus()!=AccountStatus.ACTIVE || roles==null || !roles.contains(user.getRole().name())){
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Account is not active\"}");
                    return;
                }
            }catch(IllegalArgumentException ex){
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);return;
            }
        }
        chain.doFilter(request,response);
    }
}
