package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    private UsuarioRepository repository;
    private UserDetailsService service;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        LoginSuccessHandler handler = mock(LoginSuccessHandler.class);
        service = new SecurityConfig(repository, handler).userDetailsService();
    }

    private UserDetails consultar(Usuario usuario) {
        when(repository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        return service.loadUserByUsername(usuario.getEmail().toUpperCase());
    }

    @Test
    void administradorAtivoNaoPrecisaDeUnidade() {
        Usuario admin = new Usuario("Administrador", "admin@exemplo.com", "hash", Role.ADMIN, true, null);
        UserDetails details = consultar(admin);
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void usuarioSemUnidadeNaoPodeEntrar() {
        Usuario usuario = new Usuario("Operador", "operador@exemplo.com", "hash", Role.USER, true, null);
        assertFalse(consultar(usuario).isEnabled());
    }

    @Test
    void usuarioDeUnidadeAtivaPodeEntrar() {
        Unidade unidade = new Unidade("Planta A", "Setor A", true);
        Usuario usuario = new Usuario("Operador", "operador@exemplo.com", "hash", Role.USER, true, unidade);
        UserDetails details = consultar(usuario);
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void usuarioDeUnidadeInativaNaoPodeEntrar() {
        Unidade unidade = new Unidade("Planta A", "Setor A", false);
        Usuario usuario = new Usuario("Operador", "operador@exemplo.com", "hash", Role.USER, true, unidade);
        assertFalse(consultar(usuario).isEnabled());
    }

    @Test
    void contaDesativadaNaoPodeEntrar() {
        Usuario admin = new Usuario("Administrador", "admin@exemplo.com", "hash", Role.ADMIN, false, null);
        assertFalse(consultar(admin).isEnabled());
    }

    @Test
    void emailNaoEncontradoNaoAutentica() {
        when(repository.findByEmail("inexistente@exemplo.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("  INEXISTENTE@EXEMPLO.COM "));
    }
}
