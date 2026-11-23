package com.pbe.soda_caustica_flanges.model;

import java.time.format.DateTimeFormatter;

/**
 * Projecao de leitura segura do historico de alertas para o painel e a API.
 * Jamais deduz ou confirma um vazamento a partir de um sensor nao validado.
 */
public record EventoMonitoramentoDTO(
        Long id, Long flangeId, String nomeFlange, String localizacao,
        String dataHora, String descricao, boolean ativo, String situacao) {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static EventoMonitoramentoDTO de(Incidente incidente) {
        Flange flange = incidente.getFlange();
        boolean ativo = incidente.isAlarmeAtivo();
        String descricao = Boolean.TRUE.equals(incidente.getVazamentoDetectado())
                ? "Sinal de possível anormalidade — requer verificação"
                : (incidente.getTipoOcorrencia() == null || incidente.getTipoOcorrencia().isBlank()
                    ? "Ocorrência registrada para verificação"
                    : incidente.getTipoOcorrencia());

        return new EventoMonitoramentoDTO(
                incidente.getId(),
                flange == null ? null : flange.getId(),
                flange == null ? "Flange não vinculada" : flange.getNomeFlange(),
                incidente.getLocalizacao(),
                incidente.getDataHora() == null ? "—" : incidente.getDataHora().format(FORMATO),
                descricao,
                ativo,
                !ativo ? "Normalizado"
                        : incidente.getStatusAlarme() == StatusAlarme.RECONHECIDO
                            || incidente.getStatusAlarme() == StatusAlarme.EM_MANUTENCAO
                            ? "Em acompanhamento" : "Alerta ativo"
        );
    }

    public Long getId() { return id; }
    public Long getFlangeId() { return flangeId; }
    public String getNomeFlange() { return nomeFlange; }
    public String getLocalizacao() { return localizacao; }
    public String getDataHora() { return dataHora; }
    public String getDescricao() { return descricao; }
    public boolean isAtivo() { return ativo; }
    public String getSituacao() { return situacao; }
}
