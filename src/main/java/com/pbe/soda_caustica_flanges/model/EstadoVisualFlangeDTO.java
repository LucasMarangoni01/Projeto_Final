package com.pbe.soda_caustica_flanges.model;

import java.util.List;
import java.util.Locale;

/**
 * Estado visual derivado exclusivamente de dados persistidos.
 * A classificação de vazamento confirmado exige um registro EXPLÍCITO
 * de verificação, nunca é inferida de sensor genérico ou temperatura.
 */
public record EstadoVisualFlangeDTO(
        Long flangeId, String codigo, String rotulo, String mensagem,
        int prioridade, boolean alertaAtivo) {

    public static EstadoVisualFlangeDTO classificar(
            Flange flange, List<Incidente> incidentesAtivos,
            boolean sensorInstalado, boolean possuiLeitura) {
        EstadoVisualFlangeDTO melhor = null;
        for (Incidente evento : incidentesAtivos) {
            if (evento.getFlange() == null
                    || !flange.getId().equals(evento.getFlange().getId())
                    || !evento.isAlarmeAtivo()) continue;
            String tipo = evento.getTipoOcorrencia() == null
                    ? "" : evento.getTipoOcorrencia().trim().toUpperCase(Locale.ROOT);
            EstadoVisualFlangeDTO atual;
            if ("VAZAMENTO_CONFIRMADO".equals(tipo)) {
                atual = estado(flange, "VAZAMENTO", "VAZAMENTO CONFIRMADO",
                        "Ocorrência identificada como verificada no registro; siga o protocolo operacional.", 100, true);
            } else if ("FLANGE_DANIFICADA".equals(tipo) || "DANO_FLANGE".equals(tipo)) {
                atual = estado(flange, "DANIFICADA", "FLANGE DANIFICADA",
                        "Dano registrado: isolar para avaliação da equipe responsável.", 95, true);
            } else if (Boolean.TRUE.equals(evento.getVazamentoDetectado())) {
                atual = estado(flange, "SUSPEITA", "POSSÍVEL VAZAMENTO",
                        "Sinal não verificado: investigar; não confirma vazamento de NaOH.", 90, true);
            } else if ("FALHA_SENSOR".equals(tipo) || "FALHA_COMUNICACAO".equals(tipo)) {
                atual = estado(flange, "FALHA_SENSOR", "FALHA DE SENSOR",
                        "Falha de sensor/comunicação registrada: verificar a instrumentação.", 75, true);
            } else {
                atual = estado(flange, "CRITICO", "ALERTA CRÍTICO",
                        "Ocorrência ativa no sistema: condição exige verificação.", 85, true);
            }
            if (melhor == null || atual.prioridade() > melhor.prioridade()) melhor = atual;
        }
        if (melhor != null) return melhor;

        StatusFlange status = flange.getStatus();
        if (status == StatusFlange.CRITICO) {
            return estado(flange, "CRITICO", "ESTADO CRÍTICO",
                    "Status crítico cadastrado; motivo não confirmado no monitoramento.", 85, false);
        }
        if (status == StatusFlange.ATENCAO) {
            return estado(flange, "ATENCAO", "ATENÇÃO",
                    "Status de atenção registrado para esta flange.", 40, false);
        }
        if (status == StatusFlange.MANUTENCAO) {
            return estado(flange, "MANUTENCAO", "MANUTENÇÃO",
                    "Flange marcada em manutenção.", 20, false);
        }
        if (sensorInstalado && !possuiLeitura) {
            return estado(flange, "SEM_LEITURA", "SEM LEITURA",
                    "Sensor cadastrado como instalado, mas sem leitura recebida. Comunicação não confirmada.", 30, false);
        }
        return estado(flange, "NORMAL", "NORMAL",
                sensorInstalado
                    ? "Status normal cadastrado; confirme a condição em inspeção."
                    : "Status normal cadastrado; nenhum sensor instalado comprova a condição.",
                0, false);
    }

    // JavaBeans: acesso consistente aos registros no Thymeleaf e no JSON.
    public Long getFlangeId() { return flangeId; }
    public String getCodigo() { return codigo; }
    public String getRotulo() { return rotulo; }
    public String getMensagem() { return mensagem; }
    public int getPrioridade() { return prioridade; }
    public boolean isAlertaAtivo() { return alertaAtivo; }

    private static EstadoVisualFlangeDTO estado(
            Flange flange, String codigo, String rotulo, String mensagem,
            int prioridade, boolean ativo) {
        return new EstadoVisualFlangeDTO(flange.getId(), codigo, rotulo, mensagem, prioridade, ativo);
    }
}
