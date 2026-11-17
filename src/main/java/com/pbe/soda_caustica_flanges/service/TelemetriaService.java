package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.IncidenteRepository;
import com.pbe.soda_caustica_flanges.repository.LeituraSensorRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TelemetriaService {

    private final SensorRepository sensorRepository;
    private final LeituraSensorRepository leituraRepository;
    private final IncidenteRepository incidenteRepository;
    private final SensorService sensorService;
    private final Validator validator;

    public TelemetriaService(
            SensorRepository sensorRepository,
            LeituraSensorRepository leituraRepository,
            IncidenteRepository incidenteRepository,
            SensorService sensorService,
            Validator validator) {
        this.sensorRepository = sensorRepository;
        this.leituraRepository = leituraRepository;
        this.incidenteRepository = incidenteRepository;
        this.sensorService = sensorService;
        this.validator = validator;
    }

    /**
     * Persiste uma leitura entregue pelo dispositivo através do broker MQTT.
     * O token é validado por hash e a mensagem possui chave idempotente.
     */
    @Transactional
    public ResultadoTelemetria processar(String codigoSensor, TelemetriaDTO dados) {
        validarPayload(codigoSensor, dados);

        Sensor sensor = sensorRepository.findByCodigoIdentificacaoIgnoreCase(
                codigoSensor.trim()
        ).orElseThrow(() -> new IllegalArgumentException("Sensor não cadastrado."));

        if (sensor.getStatus() != StatusSensor.INSTALADO) {
            throw new IllegalArgumentException("O sensor não está instalado e habilitado para enviar leituras.");
        }

        Flange flange = sensor.getFlange();
        if (flange == null || !flange.isAtivo()
                || flange.getUnidade() == null || !flange.getUnidade().isAtivo()) {
            throw new IllegalArgumentException("A flange vinculada está inativa ou indisponível.");
        }

        if (!sensorService.validarTokenHardware(sensor, dados.getToken())) {
            throw new IllegalArgumentException("Credencial do dispositivo inválida ou revogada.");
        }

        if (leituraRepository.existsByCodigoMensagem(dados.getIdMensagem())) {
            return new ResultadoTelemetria(null, null, false, true);
        }

        if (sensor.getTipo() != TipoSensor.TEMPERATURA_SUPERFICIE) {
            throw new IllegalArgumentException(
                    "Leituras de gás ainda não estão habilitadas. O modelo e o gás-alvo precisam ser validados; o contrato MQTT atual é legado térmico."
            );
        }

        boolean limiteAtingido = sensor.getLimiteTemperaturaAlerta() != null
                && dados.getTemperaturaC() >= sensor.getLimiteTemperaturaAlerta();

        boolean sinalHardware = Boolean.TRUE.equals(dados.getVazamentoDetectado());
        boolean alarmeDisparado = limiteAtingido || sinalHardware;

        LocalDateTime agora = LocalDateTime.now();

        LeituraSensor leitura = new LeituraSensor();
        leitura.setSensor(sensor);
        leitura.setFlange(flange);
        leitura.setRecebidaEm(agora);
        leitura.setCodigoMensagem(dados.getIdMensagem());
        leitura.setTemperatura(dados.getTemperaturaC());
        leitura.setVazamentoDetectado(dados.getVazamentoDetectado());
        leitura.setAlarmeDisparado(alarmeDisparado);
        leitura.setOrigem("MQTT");
        leitura = leituraRepository.save(leitura);

        Incidente incidente = incidenteRepository
                .findTopByFlange_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(
                        flange.getId()
                ).orElse(null);

        if (alarmeDisparado) {
            if (incidente == null) {
                incidente = criarIncidente(flange, sensor, leitura, dados, agora);
                incidente.setStatusFlangeAnterior(flange.getStatus());
            } else {
                // Atualiza a última leitura associada ao alarme ativo sem criar
                // uma nova ocorrência a cada pacote repetido do mesmo evento.
                incidente.setLeituraSensor(leitura);
                incidente.setTemperatura(dados.getTemperaturaC());
                incidente.setVazamentoDetectado(sinalHardware);
            }

            incidenteRepository.save(incidente);
            flange.setStatus(StatusFlange.CRITICO);
        } else if (incidente != null) {
            // Uma leitura normal isolada não fecha automaticamente um alarme.
            // O responsável precisa verificar a condição e registrar a normalização.
            flange.setStatus(StatusFlange.CRITICO);
        }

        return new ResultadoTelemetria(
                leitura.getId(),
                incidente == null ? null : incidente.getId(),
                alarmeDisparado,
                false
        );
    }

    private Incidente criarIncidente(
            Flange flange,
            Sensor sensor,
            LeituraSensor leitura,
            TelemetriaDTO dados,
            LocalDateTime agora) {

        Incidente incidente = new Incidente();
        incidente.setAno(agora.getYear());
        incidente.setMes(agora.getMonth());
        incidente.setQuantidade(1);
        incidente.setLocalizacao(flange.getLocalizacao());
        incidente.setDataHora(agora);
        incidente.setFlange(flange);
        incidente.setSensor(sensor);
        incidente.setLeituraSensor(leitura);
        incidente.setTipoOcorrencia("POSSIVEL_ANORMALIDADE_TEMPERATURA");
        incidente.setTemperatura(dados.getTemperaturaC());
        incidente.setVazamentoDetectado(Boolean.TRUE.equals(dados.getVazamentoDetectado()));
        incidente.setStatusAlarme(StatusAlarme.DISPARADO);
        return incidente;
    }

    private void validarPayload(String codigoSensor, TelemetriaDTO dados) {
        if (codigoSensor == null || !codigoSensor.matches("[A-Za-z0-9_-]{1,60}")) {
            throw new IllegalArgumentException("Código de sensor inválido no tópico MQTT.");
        }

        if (dados == null) {
            throw new IllegalArgumentException("Mensagem de telemetria vazia.");
        }

        Set<ConstraintViolation<TelemetriaDTO>> violacoes = validator.validate(dados);
        if (!violacoes.isEmpty()) {
            String mensagem = violacoes.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining(" "));
            throw new IllegalArgumentException(mensagem);
        }
    }

    public record ResultadoTelemetria(
            Long leituraId,
            Long incidenteId,
            boolean alarmeDisparado,
            boolean mensagemDuplicada) {
    }
}
