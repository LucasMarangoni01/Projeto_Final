package com.pbe.soda_caustica_flanges.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadoVisualFlangeDTOTest {

    // Auxiliar para os cenários de incidentes: o diagnóstico do sensor
    // é testado isoladamente em EstadoComunicacaoSensorTest.
    private EstadoVisualFlangeDTO classificar(Flange flange, List<Incidente> eventos,
                                               boolean instalado, boolean possuiLeitura) {
        EstadoComunicacaoSensor disponibilidade = instalado
                ? possuiLeitura
                    ? new EstadoComunicacaoSensor("LEITURA_RECENTE", "LEITURA RECEBIDA",
                            "Recebida recentemente", 0)
                    : new EstadoComunicacaoSensor("AGUARDANDO_DADOS", "AGUARDANDO DADOS DO SENSOR",
                            "Ainda não houve a primeira leitura", 25)
                : new EstadoComunicacaoSensor("SEM_SENSOR", "SEM SENSOR",
                        "Nenhum sensor instalado ou ativo", 10);
        return EstadoVisualFlangeDTO.classificar(flange, eventos, disponibilidade);
    }

    private Flange flange(Long id) {
        Flange flange = new Flange();
        flange.setId(id);
        flange.setNomeFlange("FL-" + id);
        flange.setStatus(StatusFlange.NORMAL);
        return flange;
    }

    private Incidente incidente(Flange flange, String tipo, Boolean sinal) {
        Incidente incidente = new Incidente();
        incidente.setFlange(flange);
        incidente.setTipoOcorrencia(tipo);
        incidente.setVazamentoDetectado(sinal);
        incidente.setDataHora(LocalDateTime.of(2026, 11, 23, 10, 0));
        incidente.setStatusAlarme(StatusAlarme.DISPARADO);
        return incidente;
    }

    @Test
    void sinalDoSensorGeraSuspeitaMasNaoVazamentoConfirmado() {
        Flange flange = flange(1L);
        EstadoVisualFlangeDTO estado = classificar(flange,
                List.of(incidente(flange, "POSSIVEL_ANORMALIDADE_TEMPERATURA", true)),
                true, true);
        assertEquals("SUSPEITA", estado.codigo());
        assertTrue(estado.rotulo().contains("POSSÍVEL"));
        assertFalse(estado.rotulo().contains("CONFIRMADO"));
        assertEquals(90, estado.prioridade());
        assertTrue(estado.alertaAtivo());
    }

    @Test
    void danoExplicitoSemSinalDeVazamentoFicaVermelhoPorMotivoCorreto() {
        Flange flange = flange(2L);
        EstadoVisualFlangeDTO estado = classificar(flange,
                List.of(incidente(flange, "FLANGE_DANIFICADA", false)), false, false);
        assertEquals("DANIFICADA", estado.codigo());
        assertEquals("FLANGE DANIFICADA", estado.rotulo());
    }

    @Test
    void falhaDeSensorRegistradaNaoViraVazamento() {
        Flange flange = flange(3L);
        EstadoVisualFlangeDTO estado = classificar(flange,
                List.of(incidente(flange, "FALHA_COMUNICACAO", false)), true, false);
        assertEquals("FALHA_SENSOR", estado.codigo());
        assertTrue(estado.mensagem().contains("comunicação"));
    }

    @Test
    void ausenciaDeLeituraNaoViraFalhaComprovada() {
        EstadoVisualFlangeDTO estado = classificar(
                flange(4L), List.of(), true, false);
        assertEquals("AGUARDANDO_DADOS", estado.codigo());
        assertFalse(estado.alertaAtivo());
    }

    @Test
    void normalRegistradoSemSensorNaoAfirmaSegurancaComprovada() {
        EstadoVisualFlangeDTO estado = classificar(
                flange(5L), List.of(), false, false);
        assertEquals("SEM_SENSOR", estado.codigo());
        assertTrue(estado.mensagem().toLowerCase().contains("nenhum sensor"));
    }

    @Test
    void multiplosAlertasPriorizamDanoSobreSuspeitaEUsamFlangeCorreta() {
        Flange a = flange(6L);
        Flange b = flange(7L);
        EstadoVisualFlangeDTO estado = classificar(a, List.of(
                incidente(b, "FLANGE_DANIFICADA", false),
                incidente(a, "POSSIVEL_ANORMALIDADE_TEMPERATURA", true),
                incidente(a, "FLANGE_DANIFICADA", false)
        ), true, true);
        assertEquals("DANIFICADA", estado.codigo());
        assertEquals(6L, estado.flangeId());
    }

    @Test
    void incidenteNormalizadoNaoPermaneceCritico() {
        Flange flange = flange(8L);
        Incidente evento = incidente(flange,"FLANGE_DANIFICADA",false);
        evento.setNormalizadoEm(LocalDateTime.of(2026,11,23,11,0));
        EstadoVisualFlangeDTO estado = classificar(flange,
                List.of(evento), true, true);
        assertEquals("NORMAL", estado.codigo());
    }

    @Test
    void estadoCriticoDoCadastroNaoEhChamadaDeVazamento() {
        Flange flange = flange(9L);
        flange.setStatus(StatusFlange.CRITICO);
        EstadoVisualFlangeDTO estado = classificar(flange,
                List.of(), false, false);
        assertEquals("CRITICO", estado.codigo());
        assertFalse(estado.alertaAtivo());
    }

    @Test
    void confirmacaoExigeTipoExplicitamenteRegistrado() {
        Flange flange = flange(10L);
        EstadoVisualFlangeDTO estado = classificar(flange,
                List.of(incidente(flange, "VAZAMENTO_CONFIRMADO", false)), true, true);
        assertEquals("VAZAMENTO", estado.codigo());
        assertTrue(estado.rotulo().contains("CONFIRMADO"));
    }
}
