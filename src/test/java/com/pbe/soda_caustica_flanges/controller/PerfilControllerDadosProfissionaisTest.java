package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.DadosProfissionaisDTO;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.FotoPerfilService;
import com.pbe.soda_caustica_flanges.service.UsuarioAuditoriaService;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerfilControllerDadosProfissionaisTest {
    private UsuarioService usuarios;
    private UsuarioAuditoriaService auditoria;
    private PerfilController controller;
    private Authentication auth;
    private BindingResult resultado;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioService.class);
        auditoria = mock(UsuarioAuditoriaService.class);
        controller = new PerfilController(usuarios, mock(FotoPerfilService.class), auditoria);
        auth = mock(Authentication.class);
        resultado = mock(BindingResult.class);
        usuario = new Usuario();
        usuario.setId(17L);
        usuario.setNome("Administrador");
        usuario.setEmail("admin@exemplo.com");
        usuario.setAtivo(true);
        when(auth.getName()).thenReturn(usuario.getEmail());
        when(usuarios.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);
    }

    @Test
    void adminAtualizaSomenteSeusDadosProfissionais() {
        usuario.setRole(Role.ADMIN);
        DadosProfissionaisDTO dados = new DadosProfissionaisDTO();
        dados.setMatricula("1234");
        dados.setCargo("Supervisor");
        dados.setSetor("Manutencao");

        String destino = controller.atualizarDadosProfissionais(
                dados, resultado, auth, new ExtendedModelMap());

        assertEquals("redirect:/perfil?dados=atualizados#dados-profissionais", destino);
        verify(usuarios).alterarDadosProfissionais(17L, dados, "admin@exemplo.com");
    }

    @Test
    void usuarioComumNaoConsegueEditarDadosPelaRotaDoAdmin() {
        usuario.setRole(Role.USER);
        DadosProfissionaisDTO dados = new DadosProfissionaisDTO();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> controller.atualizarDadosProfissionais(
                        dados, resultado, auth, new ExtendedModelMap()));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verify(usuarios, never()).alterarDadosProfissionais(anyLong(), any(), anyString());
    }

    @Test
    void entradaInvalidaNaoSalvaDadosDoAdministrador() {
        usuario.setRole(Role.ADMIN);
        when(resultado.hasErrors()).thenReturn(true);
        when(auditoria.buscarPorUsuario(17L)).thenReturn(java.util.List.of());
        DadosProfissionaisDTO dados = new DadosProfissionaisDTO();
        dados.setCargo("x".repeat(101));

        ExtendedModelMap model = new ExtendedModelMap();
        String tela = controller.atualizarDadosProfissionais(dados, resultado, auth, model);

        assertEquals("perfil/perfil", tela);
        assertSame(dados, model.get("dadosProfissionaisDTO"));
        assertNotNull(model.get("erroDadosProfissionais"));
        verify(usuarios, never()).alterarDadosProfissionais(anyLong(), any(), anyString());
    }
}
