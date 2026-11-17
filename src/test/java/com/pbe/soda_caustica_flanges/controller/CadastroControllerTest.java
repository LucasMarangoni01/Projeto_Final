package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.CadastroDTO;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CadastroControllerTest {

    @Test
    void primeiroAdministradorExigeCodigoDeInstalacaoNaVercel() {
        UsuarioService usuarios = mock(UsuarioService.class);
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        when(repositorio.count()).thenReturn(0L);
        CadastroController controller = new CadastroController(
                usuarios, repositorio, true, "chave-do-painel");

        CadastroDTO dados = new CadastroDTO();
        dados.setNome("Pessoa Teste");
        dados.setEmail("teste@exemplo.com");
        dados.setSenha("uma-senha-segura");
        dados.setConfirmaSenha("uma-senha-segura");

        ExtendedModelMap model = new ExtendedModelMap();
        String pagina = controller.cadastrar(dados, mock(BindingResult.class), "incorreta", model);
        assertEquals("cadastro/cadastro", pagina);
        assertEquals(true, model.get("exigirCodigoInstalacao"));
        assertNotNull(model.get("erro"));
        verify(usuarios, never()).cadastrar(any());
    }

    @Test
    void paginaInicialMostraCampoDeCodigoApenasParaPrimeiroCadastro() {
        UsuarioService usuarios = mock(UsuarioService.class);
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        when(repositorio.count()).thenReturn(0L);
        CadastroController controller = new CadastroController(
                usuarios, repositorio, true, "chave-do-painel");
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("cadastro/cadastro", controller.cadastro(model));
        assertEquals(true, model.get("exigirCodigoInstalacao"));
    }
}
