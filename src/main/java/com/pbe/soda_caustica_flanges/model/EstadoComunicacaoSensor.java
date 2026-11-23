package com.pbe.soda_caustica_flanges.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Diagnóstico de disponibilidade de dados, nunca diagnóstico físico de vazamento.
 * Usa a última leitura do SENSOR instalado (não a última leitura de outra
 * instrumentação que tenha compartilhado a mesma flange).
 */
public record EstadoComunicacaoSensor(String codigo, String rotulo,
                                      String mensagem, int prioridade) {

    public static EstadoComunicacaoSensor avaliar(
            List<Sensor> sensores, Map<Long, LeituraSensor> ultimasPorSensor,
            LocalDateTime agora, long intervaloMinutos) {
        Objects.requireNonNull(sensores, "sensores");
        Objects.requireNonNull(ultimasPorSensor, "ultimasPorSensor");
        Objects.requireNonNull(agora, "agora");

        if (sensores.isEmpty()) {
            return new EstadoComunicacaoSensor(
                    "SEM_SENSOR", "SEM SENSOR",
                    "Nenhum sensor cadastrado nesta flange. O status da flange não comprova segurança.",
                    10);
        }

        List<Sensor> instalados = sensores.stream()
                .filter(s -> s.getStatus() == StatusSensor.INSTALADO)
                .toList();

        if (instalados.isEmpty()) {
            return new EstadoComunicacaoSensor(
                    "NAO_INSTALADO", "SENSOR NÃO INSTALADO",
                    "Há sensor cadastrado, mas nenhum marcado como instalado. "
                            + "O cadastro não confirma conexão física nem comunicação.",
                    15);
        }

        long prazo = Math.max(1, intervaloMinutos);
        LocalDateTime limite = agora.minusMinutes(prazo);
        int semPrimeiraLeitura = 0;
        int semAtualizacao = 0;

        for (Sensor sensor : instalados) {
            LeituraSensor ultima = ultimasPorSensor.get(sensor.getId());
            if (ultima == null || ultima.getRecebidaEm() == null) {
                semPrimeiraLeitura++;
            } else if (ultima.getRecebidaEm().isBefore(limite)
                    || ultima.getRecebidaEm().isAfter(agora.plusMinutes(1))) {
                semAtualizacao++;
            }
        }

        // Precedência: um dispositivo com histórico desatualizado não deve
        // aparecer como se nunca tivesse transmitido.
        if (semAtualizacao > 0) {
            return new EstadoComunicacaoSensor(
                    "LEITURA_DESATUALIZADA", "SEM DADOS RECENTES",
                    semAtualizacao + " sensor(es) instalado(s) já enviaram leituras, "
                            + "mas não há leitura válida recente dentro da referência de "
                            + prazo + " minutos. Possível interrupção: verifique a comunicação; "
                            + "isso não comprova defeito nem vazamento.",
                    55);
        }

        if (semPrimeiraLeitura > 0) {
            return new EstadoComunicacaoSensor(
                    "AGUARDANDO_DADOS", "AGUARDANDO DADOS DO SENSOR",
                    semPrimeiraLeitura + " sensor(es) marcado(s) como instalado(s) "
                            + "ainda não enviaram a primeira leitura. A integração do "
                            + "hardware pode estar pendente; comunicação não confirmada.",
                    25);
        }

        return new EstadoComunicacaoSensor(
                "LEITURA_RECENTE", "LEITURA RECEBIDA",
                "Há leitura registrada recentemente, sem confirmação de segurança "
                        + "da flange ou de detecção de soda cáustica.",
                0);
    }
}
