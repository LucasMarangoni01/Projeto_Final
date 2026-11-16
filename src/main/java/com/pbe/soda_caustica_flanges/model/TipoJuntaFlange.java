package com.pbe.soda_caustica_flanges.model;

public enum TipoJuntaFlange {
    PTFE_EXPANDIDO("PTFE expandido"),
    GARLOCK_ANEL_RETENCAO("Garlock com anel de retenção"),
    OUTRO("Outro / não especificado");

    private final String descricao;

    TipoJuntaFlange(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
