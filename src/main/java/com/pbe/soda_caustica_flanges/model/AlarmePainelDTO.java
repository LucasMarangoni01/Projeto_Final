package com.pbe.soda_caustica_flanges.model;

import java.time.LocalDateTime;

/** Contrato de apresentação comum, com namespace independente para testes. */
public record AlarmePainelDTO(String chave, Long id, boolean teste, Long flangeId,
        String flange, String localizacao, String tipo, String gravidade,
        String estado, String rotuloEstado, boolean ativo, boolean sonoro,
        String detectadoEm, String iniciadoPor, String reconhecidoPor,
        String reconhecidoEm, String atendimentoPor, String atendimentoEm,
        String encerradoPor, String encerradoEm) {

    public static AlarmePainelDTO real(Incidente i) {
        Flange f = i.getFlange();
        String tipoRegistro = i.getTipoOcorrencia();
        String tipo = "FALHA_SENSOR".equalsIgnoreCase(tipoRegistro) || "FALHA_COMUNICACAO".equalsIgnoreCase(tipoRegistro)
                ? "FALHA_COMUNICACAO" : "VAZAMENTO_CONFIRMADO".equalsIgnoreCase(tipoRegistro)
                ? "VAZAMENTO_CONFIRMADO" : Boolean.TRUE.equals(i.getVazamentoDetectado())
                ? "SUSPEITA_VAZAMENTO" : "CRITICO";
        StatusAlarme estado = i.isAlarmeAtivo()
                ? (i.getStatusAlarme() == null ? StatusAlarme.DISPARADO : i.getStatusAlarme())
                : StatusAlarme.NORMALIZADO;
        return new AlarmePainelDTO("real:" + i.getId(), i.getId(), false,
                f == null ? null : f.getId(), f == null ? "Flange não vinculada" : f.getNomeFlange(),
                i.getLocalizacao(), tipo, tipo.equals("FALHA_COMUNICACAO") ? "ALTA" : "CRITICA",
                estado.name(), CicloAlarme.rotulo(estado), i.isAlarmeAtivo(),
                i.isAlarmeAtivo() && CicloAlarme.aguardaReconhecimento(estado),
                data(i.getDataHora()), null, i.getReconhecidoPorEmail(), data(i.getReconhecidoEm()),
                i.getAtendimentoPorEmail(), data(i.getAtendimentoEm()), i.getNormalizadoPorEmail(), data(i.getNormalizadoEm()));
    }

    public static AlarmePainelDTO teste(TesteEmergencia t) {
        boolean ativo = t.getEstado() != StatusAlarme.NORMALIZADO;
        return new AlarmePainelDTO("teste:" + t.getId(), t.getId(), true, t.getFlangeId(),
                t.getNomeFlange(), t.getLocalizacao(), t.getTipo().name(), t.getGravidade().name(),
                t.getEstado().name(), CicloAlarme.rotulo(t.getEstado()), ativo,
                ativo && CicloAlarme.aguardaReconhecimento(t.getEstado()),
                data(t.getDetectadoEm()), t.getIniciadoPor(), t.getReconhecidoPor(), data(t.getReconhecidoEm()),
                t.getAtendimentoPor(), data(t.getAtendimentoEm()), t.getEncerradoPor(), data(t.getEncerradoEm()));
    }

    private static String data(LocalDateTime data) {
        return data == null ? null : data.toString();
    }
}
