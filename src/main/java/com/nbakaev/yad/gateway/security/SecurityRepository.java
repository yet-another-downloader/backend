package com.nbakaev.yad.gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public class SecurityRepository implements ServerSecurityContextRepository {

    @Override
    public Mono<Void> save(ServerWebExchange serverWebExchange, SecurityContext securityContext) {
        //
        return null;
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange serverWebExchange) {

        // TODO: replace with LDAP/db logic
        Authentication authentication = new YadUser("ds");
        authentication.setAuthenticated(true);
        return Mono.just(new SecurityContextImpl(authentication));
    }

}
