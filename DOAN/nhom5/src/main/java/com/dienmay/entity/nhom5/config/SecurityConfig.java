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
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/products",
                                "/products/**",
                                "/login",
                                "/register",
                                "/logout",
                                "/favicon.ico",
                                "/cart",
                                "/wishlist",
                                "/about",
                                "/contact",
                                "/blog",
                                "/blog/**",
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
                                "/api/brands/**",
                                "/api/cart",
                                "/api/cart/count",
                                "/api/auth/firebase-config")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/contact",
                                "/api/ai/chat")
                        .permitAll()
                        .requestMatchers("/api/contact/my-messages").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cart/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/cart/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/cart/**").authenticated()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/orders", "/orders/**", "/checkout").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            String path = request.getRequestURI();
                            if (path.startsWith("/api/")) {
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(401);
                                response.getWriter().write(
                                        "{\"error\":\"Unauthorized\",\"message\":\"Vui lòng đăng nhập\"}"
                                );
                            } else {
                                String redirectUrl = path;
                                if (request.getQueryString() != null) {
                                    redirectUrl += "?" + request.getQueryString();
                                }
                                response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(redirectUrl, java.nio.charset.StandardCharsets.UTF_8));
                            }
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            String path = request.getRequestURI();
                            if (path.startsWith("/api/")) {
                                response.setContentType("application/json;charset=UTF-8");
                                response.setStatus(403);
                                response.getWriter().write(
                                        "{\"error\":\"Forbidden\",\"message\":\"Không có quyền truy cập\"}"
                                );
                            } else {
                                response.sendRedirect("/login?error=forbidden");
                            }
                        })
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
