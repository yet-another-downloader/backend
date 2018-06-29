package com.nbakaev.yad.gateway.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationManager implements ReactiveAuthenticationManager {
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        // JwtAuthenticationToken is my custom token.
//        if (authentication instanceof JwtAuthenticationToken) {
            authentication.setAuthenticated(true);
//        }
        return Mono.just(authentication);
    }

}
