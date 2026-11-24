package com.pbe.soda_caustica_flanges.model;

/** Regras compartilhadas por ocorrências registradas e exercícios de software. */
public final class CicloAlarme {
    private CicloAlarme() {}

    public static boolean aguardaReconhecimento(StatusAlarme status) {
        return status == null || status == StatusAlarme.DISPARADO;
    }

    public static boolean podeAtender(StatusAlarme status) {
        return status == StatusAlarme.RECONHECIDO;
    }

    public static boolean podeResolver(StatusAlarme status) {
        return status == StatusAlarme.RECONHECIDO || status == StatusAlarme.EM_MANUTENCAO;
    }

    public static String rotulo(StatusAlarme status) {
        if (aguardaReconhecimento(status)) return "Aguardando reconhecimento";
        return switch (status) {
            case RECONHECIDO -> "Reconhecido";
            case EM_MANUTENCAO -> "Em atendimento";
            case NORMALIZADO -> "Resolvido";
            default -> "Detectado";
        };
    }
}
