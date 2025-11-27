package br.com.fiap.clinic.history.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Implementação customizada de UserDetails que encapsula o ID do usuário.
 * <p>
 * Esta classe é usada para armazenar o ID numérico do usuário no contexto de segurança,
 * permitindo controle de acesso baseado no ID do paciente/médico/enfermeiro.
 */
public record CustomUserDetails(Long userId, String username,
                                Collection<? extends GrantedAuthority> authorities) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

