package com.dienmay.entity.nhom5.security;

import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.service.AuthService;
import org.springframework.core.env.Environment;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String[] AUTH_ENDPOINT_PREFIXES = {
            "/api/auth/firebase-config",
            "/api/auth/login",
            "/api/auth/register"
    };

    private final AuthService authService;
    private final Environment env;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String endpoint : AUTH_ENDPOINT_PREFIXES) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Production: only accept Firebase ID Token in Authorization header

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            String firebaseUid = decoded.getUid();

            User user = authService.loadUserByUid(firebaseUid);

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                writeJsonResponse(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "Tài khoản đã bị khóa"
                );
                return;
            }

            // Set principal as the Firebase UID (String) and map role -> authority
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + (user.getRole() != null ? user.getRole().name() : "USER"))
            );

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(firebaseUid, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), request, response);

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            writeJsonResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Token không hợp lệ hoặc đã hết hạn"
            );
        }
    }

    private void writeJsonResponse(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
