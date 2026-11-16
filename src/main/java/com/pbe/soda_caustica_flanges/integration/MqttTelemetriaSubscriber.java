package com.pbe.soda_caustica_flanges.integration;

import com.pbe.soda_caustica_flanges.model.TelemetriaDTO;
import com.pbe.soda_caustica_flanges.service.TelemetriaService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true")
public class MqttTelemetriaSubscriber implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetriaSubscriber.class);
    private static final int TAMANHO_MAXIMO_PAYLOAD = 4096;
    private static final String PREFIXO_TOPICO = "monitoramento/flanges";

    private final TelemetriaService telemetriaService;
    private final JsonMapper jsonMapper;
    private final Environment environment;

    private final String brokerUrl;
    private final String username;
    private final String password;
    private final String clientId;
    private final String topicFilter;
    private final int qos;

    private MqttClient client;

    public MqttTelemetriaSubscriber(
            TelemetriaService telemetriaService,
            JsonMapper jsonMapper,
            Environment environment,
            @Value("${app.mqtt.broker-url}") String brokerUrl,
            @Value("${app.mqtt.username:}") String username,
            @Value("${app.mqtt.password:}") String password,
            @Value("${app.mqtt.client-id}") String clientId,
            @Value("${app.mqtt.topic-filter}") String topicFilter,
            @Value("${app.mqtt.qos:1}") int qos) {
        this.telemetriaService = telemetriaService;
        this.jsonMapper = jsonMapper;
        this.environment = environment;
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.topicFilter = topicFilter;
        this.qos = qos;
    }

    @PostConstruct
    public void iniciar() {
        if (brokerUrl == null || brokerUrl.isBlank()
                || clientId == null || clientId.isBlank()
                || topicFilter == null || topicFilter.isBlank()) {
            throw new IllegalStateException("Configuração MQTT incompleta.");
        }

        if (qos < 0 || qos > 2) {
            throw new IllegalStateException("MQTT_QOS deve ser 0, 1 ou 2.");
        }

        boolean producao = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (producao && !brokerUrl.startsWith("ssl://")) {
            throw new IllegalStateException(
                    "Em produção, o broker MQTT deve usar TLS (URL iniciada por ssl://)."
            );
        }

        if (username.isBlank() != password.isBlank()) {
            throw new IllegalStateException(
                    "Configure MQTT_USERNAME e MQTT_PASSWORD juntos, ou deixe ambos vazios no ambiente local."
            );
        }

        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            if (!username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            client.connect(options);
            assinarTopico();
            log.info("Consumidor MQTT conectado e inscrito no tópico configurado.");
        } catch (MqttException e) {
            throw new IllegalStateException(
                    "Não foi possível conectar ao broker MQTT. Verifique broker, credenciais e rede.",
                    e
            );
        }
    }

    private void assinarTopico() {
        if (client == null || !client.isConnected()) {
            return;
        }

        try {
            client.subscribe(topicFilter, qos);
            log.info("Assinatura MQTT registrada (QoS {}).", qos);
        } catch (MqttException e) {
            log.error("Falha ao registrar assinatura MQTT.", e);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.info("Conexão MQTT restabelecida; renovando assinatura.");
        }
        assinarTopico();
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Conexão com o broker MQTT perdida. O cliente tentará reconectar automaticamente.");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        if (message == null || message.getPayload() == null
                || message.getPayload().length == 0
                || message.getPayload().length > TAMANHO_MAXIMO_PAYLOAD) {
            log.warn("Mensagem MQTT ignorada: tamanho de payload inválido.");
            return;
        }

        String[] partes = topic == null ? new String[0] : topic.split("/");
        if (partes.length != 4
                || !PREFIXO_TOPICO.equals(partes[0] + "/" + partes[1])
                || !"telemetria".equals(partes[3])) {
            log.warn("Mensagem MQTT ignorada: formato de tópico inesperado.");
            return;
        }

        String codigoSensor = partes[2];
        if (!codigoSensor.matches("[A-Za-z0-9_-]{1,60}")) {
            log.warn("Mensagem MQTT ignorada: código do sensor inválido no tópico.");
            return;
        }

        try {
            String json = new String(message.getPayload(), StandardCharsets.UTF_8);
            TelemetriaDTO dados = jsonMapper.readValue(json, TelemetriaDTO.class);

            TelemetriaService.ResultadoTelemetria resultado =
                    telemetriaService.processar(codigoSensor, dados);

            if (resultado.mensagemDuplicada()) {
                log.debug("Mensagem MQTT duplicada ignorada para o sensor {}.", codigoSensor);
                return;
            }

            if (resultado.alarmeDisparado()) {
                log.warn("Possível anormalidade térmica registrada para sensor {} e flange vinculada.",
                        codigoSensor);
            } else {
                log.debug("Leitura térmica registrada para o sensor {}.", codigoSensor);
            }
        } catch (Exception e) {
            // Nunca registrar o payload: ele contém o segredo individual do sensor.
            log.warn("Mensagem MQTT rejeitada para o sensor {}: {}",
                    codigoSensor,
                    e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Este consumidor não publica telemetria; não há confirmação de saída a processar.
    }

    @PreDestroy
    public void encerrar() {
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            log.warn("Não foi possível encerrar a conexão MQTT de forma limpa.");
        }
    }
}
