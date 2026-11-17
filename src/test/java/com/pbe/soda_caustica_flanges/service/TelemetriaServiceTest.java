package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelemetriaServiceTest {
    private SensorRepository sensores;
    private LeituraSensorRepository leituras;
    private IncidenteRepository incidentes;
    private SensorService sensorService;
    private ValidatorFactory validatorFactory;
    private TelemetriaService service;
    private Sensor sensor;
    private Flange flange;

    @BeforeEach
    void preparar() {
        sensores = mock(SensorRepository.class);
        leituras = mock(LeituraSensorRepository.class);
        incidentes = mock(IncidenteRepository.class);
        sensorService = mock(SensorService.class);
        validatorFactory = Validation.buildDefaultValidatorFactory();
        service = new TelemetriaService(sensores, leituras, incidentes, sensorService,
                validatorFactory.getValidator());

        Unidade unidade = new Unidade("Planta A", "Setor A", true);
        flange = new Flange();
        flange.setId(11L);
        flange.setLocalizacao("Linha A");
        flange.setUnidade(unidade);
        flange.setAtivo(true);
        flange.setStatus(StatusFlange.NORMAL);

        sensor = new Sensor();
        sensor.setFlange(flange);
        sensor.setStatus(StatusSensor.INSTALADO);
        sensor.setTipo(TipoSensor.TEMPERATURA_SUPERFICIE);
        sensor.setLimiteTemperaturaAlerta(75.0);
        when(sensores.findByCodigoIdentificacaoIgnoreCase("SENSOR_01"))
                .thenReturn(Optional.of(sensor));
    }

    @AfterEach
    void limpar() {
        validatorFactory.close();
    }

    private TelemetriaDTO mensagem(String id, double temperatura) {
        TelemetriaDTO dados = new TelemetriaDTO();
        dados.setToken("token-valido");
        dados.setIdMensagem(id);
        dados.setTemperaturaC(temperatura);
        dados.setVazamentoDetectado(false);
        return dados;
    }

    private void habilitarDispositivo(TelemetriaDTO dados) {
        when(sensorService.validarTokenHardware(sensor, dados.getToken())).thenReturn(true);
    }

    @Test
    void rejeitaIdentificadorDeMensagemInvalidoAntesDeConsultarBanco() {
        TelemetriaDTO dados = mensagem("!", 30.0);
        assertThrows(IllegalArgumentException.class, () -> service.processar("SENSOR_01", dados));
        verifyNoInteractions(leituras, incidentes);
    }

    @Test
    void rejeitaCredencialInvalidaSemRegistrarLeitura() {
        TelemetriaDTO dados = mensagem("evento_00001", 31.0);
        assertThrows(IllegalArgumentException.class, () -> service.processar("SENSOR_01", dados));
        verify(leituras, never()).save(any(LeituraSensor.class));
    }

    @Test
    void ignoraMensagemRepetidaSemDuplicarRegistros() {
        TelemetriaDTO dados = mensagem("evento_00002", 31.0);
        habilitarDispositivo(dados);
        when(leituras.existsByCodigoMensagem("evento_00002")).thenReturn(true);
        TelemetriaService.ResultadoTelemetria resultado = service.processar("SENSOR_01", dados);
        assertTrue(resultado.mensagemDuplicada());
        verify(leituras, never()).save(any(LeituraSensor.class));
        verify(incidentes, never()).save(any(Incidente.class));
    }

    @Test
    void temperaturaAltaCriaAlertaEDeixaFlangeCritica() {
        TelemetriaDTO dados = mensagem("evento_00003", 92.0);
        habilitarDispositivo(dados);
        when(leituras.save(any(LeituraSensor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(incidentes.findTopByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(11L))
                .thenReturn(Optional.empty());

        TelemetriaService.ResultadoTelemetria resultado = service.processar("SENSOR_01", dados);
        assertTrue(resultado.alarmeDisparado());
        assertEquals(StatusFlange.CRITICO, flange.getStatus());
        verify(incidentes).save(argThat(i ->
            i.getStatusAlarme() == StatusAlarme.DISPARADO && i.getTemperatura().equals(92.0)));
    }

    @Test
    void leituraNormalNaoEncerraAlertaExistente() {
        TelemetriaDTO dados = mensagem("evento_00004", 29.0);
        habilitarDispositivo(dados);
        when(leituras.save(any(LeituraSensor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(incidentes.findTopByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(11L))
                .thenReturn(Optional.of(new Incidente()));

        TelemetriaService.ResultadoTelemetria resultado = service.processar("SENSOR_01", dados);
        assertFalse(resultado.alarmeDisparado());
        assertEquals(StatusFlange.CRITICO, flange.getStatus());
        verify(incidentes, never()).save(any(Incidente.class));
    }
}
