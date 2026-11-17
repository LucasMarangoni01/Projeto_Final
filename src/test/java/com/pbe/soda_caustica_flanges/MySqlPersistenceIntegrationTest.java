package com.pbe.soda_caustica_flanges;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Requer MySQL de TESTE descartável. Nunca executado no banco local por padrão.
 * A execução é habilitada apenas quando TEST_MYSQL_URL está definida no CI.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "app.mqtt.enabled=false"
        })
@EnabledIfEnvironmentVariable(named = "TEST_MYSQL_URL", matches = ".+")
@Transactional
class MySqlPersistenceIntegrationTest {

    @Autowired private UnidadeRepository unidades;
    @Autowired private FlangeRepository flanges;
    @Autowired private SensorRepository sensores;
    @Autowired private LeituraSensorRepository leituras;
    @Autowired private IncidenteRepository incidentes;

    @Test
    void consultasDoDashboardRetornamSomenteLeiturasAtuais() {
        Unidade unidade = unidades.save(new Unidade("Teste Integração A", "Laboratório", true));
        Flange flange = criarFlange(unidade, "F-001");
        Sensor sensor = criarSensor(flange, "SNS_INTEGRACAO_001");

        List<LeituraSensor> lancamentos = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            LeituraSensor leitura = new LeituraSensor();
            leitura.setSensor(sensor);
            leitura.setFlange(flange);
            leitura.setCodigoMensagem("mysql_teste_msg_" + String.format("%04d", i));
            leitura.setTemperatura(20.0 + i);
            leitura.setRecebidaEm(LocalDateTime.of(2026, 11, 17, 9, 0).plusSeconds(i));
            lancamentos.add(leitura);
        }
        leituras.saveAllAndFlush(lancamentos);

        List<LeituraSensor> ultimas = leituras.findUltimasPorFlanges(List.of(flange.getId()));
        assertEquals(1, ultimas.size());
        assertEquals(32.0, ultimas.getFirst().getTemperatura());

        List<LeituraSensor> dezRecentes = leituras
                .findTop10ByFlange_IdInOrderByRecebidaEmDesc(List.of(flange.getId()));
        assertEquals(10, dezRecentes.size());
        assertEquals(32.0, dezRecentes.getFirst().getTemperatura());
    }

    @Test
    void incidentesPermanecemVinculadosEIsoladosPorUnidade() {
        Unidade unidadeA = unidades.save(new Unidade("Teste Integração A", "Setor A", true));
        Unidade unidadeB = unidades.save(new Unidade("Teste Integração B", "Setor B", true));
        Flange flangeA = criarFlange(unidadeA, "F-101");
        Flange flangeB = criarFlange(unidadeB, "F-102");

        Incidente incidenteA = criarIncidente(flangeA);
        Incidente incidenteB = criarIncidente(flangeB);
        incidentes.saveAllAndFlush(List.of(incidenteA, incidenteB));

        assertEquals(2, incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull());
        assertEquals(1, incidentes.countByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNull(
                unidadeA.getId()));
        assertEquals(1, incidentes.findByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
                unidadeB.getId()).size());
    }

    private Flange criarFlange(Unidade unidade, String nome) {
        Flange flange = new Flange();
        flange.setNomeFlange(nome);
        flange.setLocalizacao("Linha de teste");
        flange.setTempAmbiente(22.0);
        flange.setDataEntrada(LocalDate.of(2026, 1, 15));
        flange.setUnidade(unidade);
        flange.setStatus(StatusFlange.NORMAL);
        return flanges.saveAndFlush(flange);
    }

    private Sensor criarSensor(Flange flange, String codigo) {
        Sensor sensor = new Sensor();
        sensor.setCodigoIdentificacao(codigo);
        sensor.setTipo(TipoSensor.TEMPERATURA_SUPERFICIE);
        sensor.setFlange(flange);
        sensor.setStatus(StatusSensor.INSTALADO);
        return sensores.saveAndFlush(sensor);
    }

    private Incidente criarIncidente(Flange flange) {
        Incidente incidente = new Incidente();
        incidente.setAno(2026);
        incidente.setMes(java.time.Month.NOVEMBER);
        incidente.setQuantidade(1);
        incidente.setLocalizacao("Linha de teste");
        incidente.setDataHora(LocalDateTime.of(2026, 11, 17, 10, 30));
        incidente.setFlange(flange);
        incidente.setStatusAlarme(StatusAlarme.DISPARADO);
        return incidente;
    }
}
