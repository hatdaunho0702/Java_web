package com.dienmay.entity.nhom5.security;

import com.dienmay.entity.nhom5.entity.Role;
import com.dienmay.entity.nhom5.entity.User;
import com.dienmay.entity.nhom5.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            String firebaseUid = decoded.getUid();

            User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
            if (user == null) {
                writeJsonResponse(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized",
                        "Tài khoản không tồn tại trong hệ thống"
                );
                return;
            }

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                writeJsonResponse(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden",
                        "Tài khoản đã bị khóa"
                );
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(mapRoleToAuthority(user.getRole())));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(firebaseUid, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

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

    private String mapRoleToAuthority(Role role) {
        if (role == Role.ADMIN) {
            return "ROLE_ADMIN";
        }
        if (role == Role.STAFF) {
            return "ROLE_STAFF";
        }
        return "ROLE_CUSTOMER";
    }

    private void writeJsonResponse(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
