package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoramentoResumoControllerTest {
    private UsuarioRepository usuarios;
    private IncidenteRepository incidentes;
    private LeituraSensorRepository leituras;
    private MonitoramentoResumoController controller;
    private Authentication auth;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        incidentes = mock(IncidenteRepository.class);
        leituras = mock(LeituraSensorRepository.class);
        auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("operador@exemplo.com");
        controller = new MonitoramentoResumoController(usuarios, incidentes, leituras);
    }

    @Test
    void usuarioConsultaSomenteSuaUnidade() {
        Unidade unidade = new Unidade("A", "Linha A", true);
        unidade.setId(15L);
        Usuario usuario = new Usuario("Operador", "operador@exemplo.com", "hash",
                Role.USER, true, unidade);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(incidentes.countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(15L))
                .thenReturn(2L);
        LeituraSensor leitura = new LeituraSensor();
        leitura.setRecebidaEm(LocalDateTime.of(2026, 11, 17, 10, 20));
        when(leituras.findTopByFlange_Unidade_IdOrderByRecebidaEmDesc(15L))
                .thenReturn(Optional.of(leitura));

        MonitoramentoResumoController.Resumo resposta = controller.resumo(auth).getBody();
        assertNotNull(resposta);
        assertEquals(2L, resposta.alertasAtivos());
        assertEquals("2026-11-17T10:20", resposta.ultimaLeituraEm());
        verify(incidentes, never()).countByDataHoraIsNotNullAndNormalizadoEmIsNull();
        verify(leituras, never()).findTopByOrderByRecebidaEmDesc();
    }

    @Test
    void adminConsultaDadosGlobais() {
        Usuario admin = new Usuario("Admin", "operador@exemplo.com", "hash",
                Role.ADMIN, true, null);
        when(usuarios.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull()).thenReturn(7L);

        MonitoramentoResumoController.Resumo resposta = controller.resumo(auth).getBody();
        assertNotNull(resposta);
        assertEquals(7L, resposta.alertasAtivos());
        assertNull(resposta.ultimaLeituraEm());
        verify(leituras).findTopByOrderByRecebidaEmDesc();
    }

    @Test
    void usuarioSemUnidadeNaoPodeConsultar() {
        Usuario usuario = new Usuario("Operador", "operador@exemplo.com", "hash",
                Role.USER, true, null);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        ResponseStatusException erro =
                assertThrows(ResponseStatusException.class, () -> controller.resumo(auth));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        verifyNoInteractions(incidentes, leituras);
    }

    @Test
    void contaInativaNaoPodeConsultar() {
        Usuario usuario = new Usuario("Admin", "operador@exemplo.com", "hash",
                Role.ADMIN, false, null);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        assertThrows(ResponseStatusException.class, () -> controller.resumo(auth));
        verifyNoInteractions(incidentes, leituras);
    }
}
