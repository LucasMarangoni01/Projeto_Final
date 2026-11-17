package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SensorServiceEscopoGasTest {
    private SensorRepository sensores;
    private FlangeRepository flanges;
    private SensorService servico;

    @BeforeEach
    void preparar() {
        sensores = mock(SensorRepository.class);
        flanges = mock(FlangeRepository.class);
        servico = new SensorService(sensores, flanges);
    }

    @Test
    void novoCadastroTermicoEhBloqueadoSemAcessarBanco() {
        SensorDTO dto = new SensorDTO();
        dto.setTipo(TipoSensor.TEMPERATURA_SUPERFICIE);
        dto.setCodigoIdentificacao("LEGADO-01");
        dto.setFlangeId(1L);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.salvar(dto));
        assertTrue(erro.getMessage().contains("somente sensores de gás"));
        verifyNoInteractions(sensores, flanges);
    }

    @Test
    void novoCadastroDeGasPermaneceNoInventarioSemGerarTelemetria() {
        SensorDTO dto = new SensorDTO();
        dto.setCodigoIdentificacao("gas-001");
        dto.setFlangeId(5L);
        dto.setStatus(StatusSensor.CADASTRADO);
        assertEquals(TipoSensor.DETECCAO_GAS, dto.getTipo());

        Flange flange = new Flange();
        flange.setId(5L);
        when(flanges.findById(5L)).thenReturn(Optional.of(flange));
        when(sensores.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sensor salvo = servico.salvar(dto);
        assertEquals(TipoSensor.DETECCAO_GAS, salvo.getTipo());
        assertEquals("GAS-001", salvo.getCodigoIdentificacao());
        assertEquals(flange, salvo.getFlange());
        assertNull(salvo.getTokenHardwareHash());
        verify(sensores).save(salvo);
    }

    @Test
    void historicoTermicoPodeSerEditadoSemPerderOTipoAntigo() {
        Flange flange = new Flange();
        flange.setId(9L);
        Sensor existente = new Sensor();
        existente.setId(3L);
        existente.setFlange(flange);
        existente.setTipo(TipoSensor.TEMPERATURA_SUPERFICIE);
        existente.setStatus(StatusSensor.CADASTRADO);

        SensorDTO dto = new SensorDTO(existente);
        dto.setCodigoIdentificacao("OLD-03");
        when(sensores.findById(3L)).thenReturn(Optional.of(existente));
        when(flanges.findById(9L)).thenReturn(Optional.of(flange));
        when(sensores.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sensor salvo = servico.salvar(dto);
        assertEquals(TipoSensor.TEMPERATURA_SUPERFICIE, salvo.getTipo());
        verify(sensores).save(salvo);
    }

    @Test
    void naoEmiteTokenParaGasNaoHomologado() {
        Sensor existente = new Sensor();
        existente.setId(12L);
        existente.setTipo(TipoSensor.DETECCAO_GAS);
        existente.setStatus(StatusSensor.INSTALADO);
        when(sensores.findById(12L)).thenReturn(Optional.of(existente));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.gerarTokenHardware(12L));
        assertTrue(erro.getMessage().contains("não está homologada"));
        verify(sensores, never()).save(any(Sensor.class));
    }
}
