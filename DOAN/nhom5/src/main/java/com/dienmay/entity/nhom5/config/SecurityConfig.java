package com.dienmay.entity.nhom5.config;

import com.dienmay.entity.nhom5.security.FirebaseTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/",
            "/products",
                "/login",
                "/cart",
                "/checkout",
            "/orders",
                "/orders/**",
                "/products/**",
                "/admin/**",
                "/css/**",
                "/js/**",
                "/assets/**",
                "/images/**",
                "/uploads/**",
                "/webjars/**"
            )
            .permitAll()
            .requestMatchers(HttpMethod.GET,
                "/api/products/**",
                "/api/categories/**",
                "/api/brands/**")
            .permitAll()
            .requestMatchers(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**")
            .permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, e) -> {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(401);
                response.getWriter().write(
                    "{\"error\":\"Unauthorized\",\"message\":\"Vui lòng đăng nhập\"}"
                );
            })
            .accessDeniedHandler((request, response, e) -> {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(403);
                response.getWriter().write(
                    "{\"error\":\"Forbidden\",\"message\":\"Không có quyền truy cập\"}"
                );
            })
        )
        .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
