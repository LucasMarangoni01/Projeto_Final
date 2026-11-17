package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import com.pbe.soda_caustica_flanges.service.RelatorioCsvService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelatorioControllerTest {

    private UsuarioRepository usuarios;
    private FlangeRepository flanges;
    private FuncionarioRepository funcionarios;
    private ManutencaoRepository manutencoes;
    private IncidenteRepository incidentes;
    private LeituraSensorRepository leituras;
    private Authentication autenticacao;
    private RelatorioController controller;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        flanges = mock(FlangeRepository.class);
        funcionarios = mock(FuncionarioRepository.class);
        manutencoes = mock(ManutencaoRepository.class);
        incidentes = mock(IncidenteRepository.class);
        leituras = mock(LeituraSensorRepository.class);
        autenticacao = mock(Authentication.class);

        when(autenticacao.getName()).thenReturn("operador@empresa.com.br");
        controller = new RelatorioController(usuarios, flanges, funcionarios,
                manutencoes, incidentes, leituras, new RelatorioCsvService());
    }

    @Test
    void usuarioExportaSomenteFlangesDaSuaUnidade() {
        Unidade unidade = new Unidade("Planta A", "Linha A", true);
        unidade.setId(23L);
        Usuario usuario = new Usuario("Operador", "operador@empresa.com.br",
                "hash", Role.USER, true, unidade);
        when(usuarios.findByEmail("operador@empresa.com.br")).thenReturn(Optional.of(usuario));

        Flange flange = new Flange();
        flange.setId(12L);
        flange.setNomeFlange("F-12");
        flange.setLocalizacao("Linha A");
        flange.setUnidade(unidade);
        when(flanges.findByUnidadeIdOrderByIdAsc(23L)).thenReturn(List.of(flange));

        ResponseEntity<byte[]> resposta = controller.exportar("flanges", autenticacao);
        String conteudo = new String(resposta.getBody(), StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("F-12"));
        assertTrue(conteudo.contains("Planta A"));
        verify(flanges).findByUnidadeIdOrderByIdAsc(23L);
        verify(flanges, never()).findAll();
    }

    @Test
    void adminExportaDeTodasAsUnidades() {
        Usuario admin = new Usuario("Administrador", "operador@empresa.com.br",
                "hash", Role.ADMIN, true, null);
        when(usuarios.findByEmail("operador@empresa.com.br")).thenReturn(Optional.of(admin));
        when(flanges.findAll()).thenReturn(List.of());

        controller.exportar("flanges", autenticacao);
        verify(flanges).findAll();
        verify(flanges, never()).findByUnidadeIdOrderByIdAsc(anyLong());
    }

    @Test
    void usuarioSemUnidadeAtivaNaoPodeExportar() {
        Usuario semUnidade = new Usuario("Operador", "operador@empresa.com.br",
                "hash", Role.USER, true, null);
        when(usuarios.findByEmail("operador@empresa.com.br")).thenReturn(Optional.of(semUnidade));

        assertThrows(ResponseStatusException.class,
                () -> controller.exportar("flanges", autenticacao));
        verifyNoInteractions(flanges, funcionarios, manutencoes, incidentes, leituras);
    }

    @Test
    void tipoDesconhecidoNaoRetornaDados() {
        Usuario admin = new Usuario("Administrador", "operador@empresa.com.br",
                "hash", Role.ADMIN, true, null);
        when(usuarios.findByEmail("operador@empresa.com.br")).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class,
                () -> controller.exportar("invalido", autenticacao));
        verifyNoInteractions(flanges, funcionarios, manutencoes, incidentes, leituras);
    }
}
