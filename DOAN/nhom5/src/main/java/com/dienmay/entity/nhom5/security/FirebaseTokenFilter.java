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

    private static final String[] STATIC_PATHS = {
            "/css/", "/js/", "/assets/", "/uploads/", "/favicon.ico", "/webjars/"
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
        for (String staticPath : STATIC_PATHS) {
            if (path.startsWith(staticPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            // Check session authentication if it exists
            org.springframework.security.core.Authentication sessionAuth = SecurityContextHolder.getContext().getAuthentication();
            if (sessionAuth != null && sessionAuth.isAuthenticated() && !"anonymousUser".equals(sessionAuth.getPrincipal())) {
                Object principal = sessionAuth.getPrincipal();
                String uid = null;
                if (principal instanceof String) {
                    uid = (String) principal;
                } else if (principal instanceof com.dienmay.entity.nhom5.security.CustomUserDetails cud && cud.getUser() != null) {
                    uid = cud.getUser().getUid();
                }
                if (uid != null) {
                    try {
                        User user = authService.loadUserByUid(uid);
                        if (!Boolean.TRUE.equals(user.getIsActive())) {
                            SecurityContextHolder.clearContext();
                            if (request.getSession(false) != null) {
                                request.getSession().invalidate();
                            }
                            if (request.getRequestURI().startsWith("/api/")) {
                                writeJsonResponse(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden",
                                        "Tài khoản đã bị khóa"
                                );
                            } else {
                                response.sendRedirect("/login?error=blocked");
                            }
                            return;
                        }
                    } catch (Exception ex) {
                        SecurityContextHolder.clearContext();
                        if (request.getSession(false) != null) {
                            request.getSession().invalidate();
                        }
                        response.sendRedirect("/login");
                        return;
                    }
                }
            }
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
