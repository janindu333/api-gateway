// package com.baber.apigateway.configuration;

// import org.keycloak.KeycloakPrincipal;
// import org.keycloak.KeycloakSecurityContext;
// import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
// import org.keycloak.representations.AccessToken;
// import org.keycloak.representations.idm.RoleRepresentation;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
// import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
// import org.springframework.security.config.web.server.ServerHttpSecurity;
// import org.springframework.security.web.server.SecurityWebFilterChain;
// import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
// import springfox.documentation.service.Server;

// import java.util.List;
// import java.util.stream.Collectors;

// @Configuration
// @EnableWebFluxSecurity
// public class SecurityConfig {

//     @Bean
//     public SecurityWebFilterChain springSecurityWebFilterChain(ServerHttpSecurity serverHttpSecurity){
//         serverHttpSecurity.csrf().disable()
//                 .authorizeExchange(
//                     exchange -> 
//                     exchange
//                     .pathMatchers("/eureka/**").permitAll()
//                     .pathMatchers("/test").permitAll()
//                         .pathMatchers("/products/**").hasAuthority("user")
//                         .anyExchange()
//                         .authenticated())
//                 .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);
//         return serverHttpSecurity.build();
//     }
// }
