package com.pbe.soda_caustica_flanges.model;

public enum TipoFaceFlange {
    RF("RF — Raised Face"),
    RTJ("RTJ — Ring Type Joint");

    private final String descricao;

    TipoFaceFlange(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
