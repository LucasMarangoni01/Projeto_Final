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
    @Autowired private TesteEmergenciaRepository testes;
    @Autowired private jakarta.persistence.EntityManager em;

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

    @Test
    void exercicioPersistidoNaoEntraNasEstatisticasNemEncerraAlarmeReal() {
        Unidade unidade = unidades.save(new Unidade("Teste Exercício", "Setor A", true));
        Flange flange = criarFlange(unidade, "F-201");
        Incidente incidente = incidentes.saveAndFlush(criarIncidente(flange));
        Long idReal = incidente.getId();
        TesteEmergencia teste = testes.saveAndFlush(new TesteEmergencia(flange,
                TesteEmergencia.Tipo.VAZAMENTO, TesteEmergencia.Gravidade.CRITICA,
                "admin@example.com", LocalDateTime.now()));
        Long idTeste = teste.getId();
        assertEquals(1, incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull());
        assertEquals(1, testes.findByEstadoNotOrderByDetectadoEmDesc(StatusAlarme.NORMALIZADO).size());
        teste.reconhecer("admin@example.com", LocalDateTime.now());
        teste.atender("admin@example.com", LocalDateTime.now());
        teste.encerrar("admin@example.com", LocalDateTime.now());
        testes.saveAndFlush(teste);
        em.clear();
        assertTrue(incidentes.findById(idReal).orElseThrow().isAlarmeAtivo());
        assertEquals(StatusFlange.NORMAL, flanges.findById(flange.getId()).orElseThrow().getStatus());
        assertEquals(StatusAlarme.NORMALIZADO, testes.findById(idTeste).orElseThrow().getEstado());
        assertEquals(1, testes.findTop20ByEstadoOrderByDetectadoEmDesc(StatusAlarme.NORMALIZADO).size());
        assertEquals(1, incidentes.countByDataHoraIsNotNullAndNormalizadoEmIsNull());
    }

    @Test
    void reconhecimentoEAtendimentoAtomicosNaoNormalizamOcorrencia() {
        Unidade unidade = unidades.save(new Unidade("Teste Atendimento", "Setor B", true));
        Flange flange = criarFlange(unidade, "F-202");
        Long id = incidentes.saveAndFlush(criarIncidente(flange)).getId();
        LocalDateTime agora = LocalDateTime.now();
        assertEquals(1, incidentes.reconhecerAtivo(id, StatusAlarme.DISPARADO, StatusAlarme.RECONHECIDO, agora, "operador@example.com", "Verificação"));
        assertEquals(0, incidentes.reconhecerAtivo(id, StatusAlarme.DISPARADO, StatusAlarme.RECONHECIDO, agora, "operador@example.com", "Repetido"));
        assertEquals(1, incidentes.marcarEmAtendimento(id, StatusAlarme.RECONHECIDO, StatusAlarme.EM_MANUTENCAO, agora, "operador@example.com"));
        em.clear();
        Incidente atualizado = incidentes.findById(id).orElseThrow();
        assertTrue(atualizado.isAlarmeAtivo());
        assertEquals(StatusAlarme.EM_MANUTENCAO, atualizado.getStatusAlarme());
        assertEquals("operador@example.com", atualizado.getAtendimentoPorEmail());
        assertEquals("Verificação", atualizado.getJustificativaReconhecimento());
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
