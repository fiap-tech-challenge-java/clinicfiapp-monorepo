package br.com.fiap.clinic.scheduler.controller.graphql;

import br.com.fiap.clinic.scheduler.config.security.TokenService;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class AuthGraphQLController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    // Record para resposta
    public record AuthPayload(String token, String type) {}
    // Record para entrada
    public record LoginInput(String login, String password) {}

    @MutationMapping
    public AuthPayload login(@Argument LoginInput input) {
        // O AuthenticationManager vai usar o UserDetailsService (AuthorizationService) que já existe
        UsernamePasswordAuthenticationToken loginToken =
                new UsernamePasswordAuthenticationToken(input.login(), input.password());

        Authentication auth = authenticationManager.authenticate(loginToken);

        // Se chegou aqui, a senha está correta. Gera o Token.
        String token = tokenService.generateToken((User) auth.getPrincipal());

        return new AuthPayload(token, "Bearer");
    }
}