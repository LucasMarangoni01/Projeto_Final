package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomeControllerTest {
    private FlangeRepository flanges;
    private SensorRepository sensores;
    private UsuarioRepository usuarios;
    private LeituraSensorRepository leituras;
    private IncidenteRepository incidentes;
    private HomeController controller;
    private Authentication auth;

    @BeforeEach
    void preparar() {
        flanges = mock(FlangeRepository.class);
        sensores = mock(SensorRepository.class);
        usuarios = mock(UsuarioRepository.class);
        leituras = mock(LeituraSensorRepository.class);
        incidentes = mock(IncidenteRepository.class);
        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("operador@exemplo.com");
        controller = new HomeController(flanges, sensores, usuarios, leituras, incidentes);
    }

    @Test
    void painelVazioNaoConsultaLeiturasDoHistorico() {
        Usuario admin = new Usuario("Admin", "operador@exemplo.com", "hash",
                Role.ADMIN, true, null);
        when(usuarios.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(flanges.findAtivas()).thenReturn(new ArrayList<>());
        when(incidentes.findByDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc())
                .thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("home/index", controller.home(model, auth));
        assertEquals(0, model.get("totalFlanges"));
        assertEquals(0, model.get("totalAlertasAtivos"));
        verifyNoInteractions(leituras, sensores);
    }

    @Test
    void usuarioConsultaLeiturasLimitadasSomenteDasFlangesPermitidas() {
        Unidade unidade = new Unidade("Unidade A", "Linha A", true);
        unidade.setId(8L);
        Usuario usuario = new Usuario("Operador", "operador@exemplo.com", "hash",
                Role.USER, true, unidade);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        Flange flange = new Flange();
        flange.setId(13L);
        flange.setUnidade(unidade);
        flange.setNomeFlange("FL-013");
        flange.setLocalizacao("Linha A");
        flange.setTempAmbiente(25.0);
        flange.setDataEntrada(LocalDate.of(2026, 1, 1));
        when(flanges.findAtivasPorUnidade(8L)).thenReturn(new ArrayList<>(List.of(flange)));
        when(sensores.findByFlange_IdIn(List.of(13L))).thenReturn(List.of());

        LeituraSensor leitura = new LeituraSensor();
        leitura.setFlange(flange);
        leitura.setTemperatura(29.5);
        leitura.setRecebidaEm(LocalDateTime.of(2026, 11, 17, 10, 29));
        when(leituras.findTop10ByFlange_IdInOrderByRecebidaEmDesc(List.of(13L)))
                .thenReturn(List.of(leitura));
        when(incidentes.findByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(8L))
                .thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("home/index", controller.home(model, auth));
        assertEquals(1, model.get("totalFlanges"));
        assertEquals(List.of(leitura), model.get("leiturasRecentes"));
        verify(flanges, never()).findAtivas();
        verify(leituras, never()).findUltimasPorSensores(anyList());
        verify(leituras).findTop10ByFlange_IdInOrderByRecebidaEmDesc(List.of(13L));
    }
}
