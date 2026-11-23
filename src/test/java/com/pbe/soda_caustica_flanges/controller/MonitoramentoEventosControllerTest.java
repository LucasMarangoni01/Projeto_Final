package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoramentoEventosControllerTest {
    private UsuarioRepository usuarios;
    private IncidenteRepository incidentes;
    private MonitoramentoEventosController controller;
    private Authentication autenticacao;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        incidentes = mock(IncidenteRepository.class);
        autenticacao = mock(Authentication.class);
        when(autenticacao.isAuthenticated()).thenReturn(true);
        when(autenticacao.getName()).thenReturn("operador@exemplo.com");
        controller = new MonitoramentoEventosController(usuarios, incidentes);
    }

    @Test
    void usuarioRecebeSomenteAlertasDaUnidadePermitida() {
        Unidade unidade = new Unidade("Linha A", "Setor A", true);
        unidade.setId(18L);
        Usuario user = new Usuario("Operador", "operador@exemplo.com", "hash",
                Role.USER, true, unidade);
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        Flange flange = new Flange();
        flange.setId(52L);
        flange.setNomeFlange("FL-052");
        Incidente alerta = new Incidente();
        alerta.setId(91L);
        alerta.setFlange(flange);
        alerta.setLocalizacao("Setor A");
        alerta.setDataHora(LocalDateTime.of(2026, 11, 23, 10, 10));
        alerta.setStatusAlarme(StatusAlarme.DISPARADO);

        when(incidentes.findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(18L))
                .thenReturn(List.of(alerta));
        when(incidentes.findIdsFlangesComAlertasAtivosPorUnidade(18L))
                .thenReturn(List.of(52L));
        when(incidentes.countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(18L))
                .thenReturn(1L);

        MonitoramentoEventosController.Painel painel = controller.listar(autenticacao).getBody();
        assertNotNull(painel);
        assertEquals(1, painel.eventos().size());
        assertEquals("FL-052", painel.eventos().getFirst().nomeFlange());
        assertTrue(painel.eventos().getFirst().ativo());
        assertEquals(List.of(52L), painel.idsFlangesEmAlerta());
        assertEquals(1L, painel.totalAlertasAtivos());
        verify(incidentes, never()).findTop6ByDataHoraIsNotNullOrderByDataHoraDesc();
        verify(incidentes, never()).findIdsFlangesComAlertasAtivos();
    }

    @Test
    void adminPodeConsultarHistoricoGlobal() {
        Usuario admin = new Usuario("Admin", "operador@exemplo.com", "hash",
                Role.ADMIN, true, null);
        when(usuarios.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(incidentes.findTop6ByDataHoraIsNotNullOrderByDataHoraDesc())
                .thenReturn(List.of());
        when(incidentes.findIdsFlangesComAlertasAtivos()).thenReturn(List.of(10L));
        when(incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull()).thenReturn(2L);

        MonitoramentoEventosController.Painel painel = controller.listar(autenticacao).getBody();
        assertNotNull(painel);
        assertEquals(2L, painel.totalAlertasAtivos());
        assertEquals(List.of(10L), painel.idsFlangesEmAlerta());
        verify(incidentes).findTop6ByDataHoraIsNotNullOrderByDataHoraDesc();
    }

    @Test
    void contaSemUnidadeNaoAcessaAlertas() {
        Usuario user = new Usuario("Operador", "operador@exemplo.com", "hash",
                Role.USER, true, null);
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.listar(autenticacao));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(incidentes);
    }

    @Test
    void usuarioInativoNaoAcessaAlertas() {
        Usuario user = new Usuario("Operador", "operador@exemplo.com", "hash",
                Role.ADMIN, false, null);
        when(usuarios.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.listar(autenticacao));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(incidentes);
    }

    @Test
    void descricaoDeEventoNaoConfirmaVazamentoAutomaticamente() {
        Incidente alerta = new Incidente();
        alerta.setId(8L);
        alerta.setLocalizacao("Linha de testes");
        alerta.setDataHora(LocalDateTime.of(2026, 11, 23, 11, 0));
        alerta.setVazamentoDetectado(true);
        alerta.setStatusAlarme(StatusAlarme.DISPARADO);

        EventoMonitoramentoDTO dto = EventoMonitoramentoDTO.de(alerta);
        assertTrue(dto.ativo());
        assertTrue(dto.descricao().contains("possível anormalidade"));
        assertFalse(dto.descricao().toLowerCase().contains("confirmado"));
    }
}
