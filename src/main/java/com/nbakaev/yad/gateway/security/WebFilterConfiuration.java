package com.nbakaev.yad.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebFilterConfiuration {

    @Bean
    WebFilter webFilter () {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("Access-Control-Max-Age", "3600");
            response.getHeaders().add("Access-Control-Allow-Credentials", "true");

//            if (request.getMethod().equals("OPTIONS")) {
//                response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
//                response.addHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, Date, X-Date, Authorization".concat("," + SYSTEM_CONSTS.X_biqa_Version_Hash_HEADER));
//                response.setHeader("X-Frame-Options", "DENY"); //SAMEORIGIN
//
//                // if http request is options - do not process filters chain after
//                // because we have user authentication filter and it will fail
//                // with exception
//                return;
//            }

            // CORS, allow all use our API via Ajax
            response.getHeaders().add("Access-Control-Allow-Origin", exchange.getRequest().getHeaders().get("Origin").get(0));
            return chain.filter(exchange);
        };
    }

}
