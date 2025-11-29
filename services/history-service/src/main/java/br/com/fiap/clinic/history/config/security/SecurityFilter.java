package br.com.fiap.clinic.history.config.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        if (token != null) {
            DecodedJWT decodedJWT = tokenService.validateToken(token);

            if (decodedJWT != null) {
                String login = decodedJWT.getSubject();
                String role = decodedJWT.getClaim("role").asString();
                String userId = decodedJWT.getClaim("userId").asString();

                // Monta o usuário apenas com dados do Token (Sem DB!)
                // Usamos o CustomUserDetails que já existe no seu projeto, ajustado para ID String/UUID
                CustomUserDetails user = new CustomUserDetails(
                        UUID.fromString(userId), // Converte String do token para UUID
                        login,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                );

                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}