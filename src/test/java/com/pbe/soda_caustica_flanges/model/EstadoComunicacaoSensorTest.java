package com.pbe.soda_caustica_flanges.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstadoComunicacaoSensorTest {
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 14, 0);

    private Sensor sensor(long id, StatusSensor status) {
        Sensor sensor = new Sensor();
        sensor.setId(id);
        sensor.setStatus(status);
        return sensor;
    }

    private LeituraSensor leitura(Sensor sensor, LocalDateTime horario) {
        LeituraSensor leitura = new LeituraSensor();
        leitura.setSensor(sensor);
        leitura.setRecebidaEm(horario);
        return leitura;
    }

    @Test
    void semSensorCadastradoNaoApareceComoFalhaOuNormal() {
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(), Map.of(), AGORA, 15);
        assertEquals("SEM_SENSOR", estado.codigo());
        assertEquals("SEM SENSOR", estado.rotulo());
        assertTrue(estado.mensagem().contains("não comprova segurança"));
    }

    @Test
    void sensorCadastradoMasNaoInstaladoTemEstadoEspecifico() {
        Sensor registrado = sensor(1, StatusSensor.CADASTRADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(registrado), Map.of(), AGORA, 15);
        assertEquals("NAO_INSTALADO", estado.codigo());
        assertEquals("SENSOR NÃO INSTALADO", estado.rotulo());
    }

    @Test
    void sensorInstaladoSemPrimeiraLeituraAguardaDados() {
        Sensor instalado = sensor(2, StatusSensor.INSTALADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(instalado), Map.of(), AGORA, 15);
        assertEquals("AGUARDANDO_DADOS", estado.codigo());
        assertEquals("AGUARDANDO DADOS DO SENSOR", estado.rotulo());
        assertTrue(estado.mensagem().contains("primeira leitura"));
    }

    @Test
    void leituraAnteriorAReferenciaSinalizaDadosDesatualizados() {
        Sensor instalado = sensor(3, StatusSensor.INSTALADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(instalado), Map.of(3L, leitura(instalado, AGORA.minusMinutes(16))),
                AGORA, 15);
        assertEquals("LEITURA_DESATUALIZADA", estado.codigo());
        assertEquals("SEM DADOS RECENTES", estado.rotulo());
        assertTrue(estado.mensagem().contains("Possível interrupção"));
    }

    @Test
    void leituraNoLimiteAindaContaComoRecente() {
        Sensor instalado = sensor(4, StatusSensor.INSTALADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(instalado), Map.of(4L, leitura(instalado, AGORA.minusMinutes(15))),
                AGORA, 15);
        assertEquals("LEITURA_RECENTE", estado.codigo());
    }

    @Test
    void leituraDeOutroSensorNaoComprovaPrimeiraLeituraDoInstalado() {
        Sensor instalado = sensor(5, StatusSensor.INSTALADO);
        Sensor sensorAntigo = sensor(6, StatusSensor.DESATIVADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(instalado, sensorAntigo),
                Map.of(6L, leitura(sensorAntigo, AGORA.minusMinutes(1))), AGORA, 15);
        assertEquals("AGUARDANDO_DADOS", estado.codigo());
    }

    @Test
    void multiplosSensoresComUmSemDadosNaoAparecemComoTodosOperando() {
        Sensor comLeitura = sensor(7, StatusSensor.INSTALADO);
        Sensor semLeitura = sensor(8, StatusSensor.INSTALADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(comLeitura, semLeitura),
                Map.of(7L, leitura(comLeitura, AGORA.minusMinutes(1))), AGORA, 15);
        assertEquals("AGUARDANDO_DADOS", estado.codigo());
        assertTrue(estado.mensagem().contains("1 sensor(es)"));
    }

    @Test
    void dadosAntigosTêmPrioridadeSobrePrimeiraLeituraAusente() {
        Sensor antigo = sensor(9, StatusSensor.INSTALADO);
        Sensor novo = sensor(10, StatusSensor.INSTALADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(antigo, novo),
                Map.of(9L, leitura(antigo, AGORA.minusMinutes(20))), AGORA, 15);
        assertEquals("LEITURA_DESATUALIZADA", estado.codigo());
    }

    @Test
    void dataFuturaMuitoAdiantadaNaoComprovaLeituraRecente() {
        Sensor instalado = sensor(11, StatusSensor.INSTALADO);
        EstadoComunicacaoSensor estado = EstadoComunicacaoSensor.avaliar(
                List.of(instalado), Map.of(11L, leitura(instalado, AGORA.plusMinutes(3))),
                AGORA, 15);
        assertEquals("LEITURA_DESATUALIZADA", estado.codigo());
    }
}
