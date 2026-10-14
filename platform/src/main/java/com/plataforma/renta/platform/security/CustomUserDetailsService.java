package com.plataforma.renta.platform.security;

import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getEmail().getValor())
                .password(usuario.getPasswordHash())
                .authorities(usuario.getRol().name())
                .build();
    }
}