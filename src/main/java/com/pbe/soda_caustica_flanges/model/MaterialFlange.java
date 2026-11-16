package com.pbe.soda_caustica_flanges.model;

public enum MaterialFlange {
    ASTM_A105("Aço carbono ASTM A105"),
    AISI_316L("Aço inoxidável AISI 316L"),
    MONEL("Monel"),
    OUTRO("Outro / não especificado");

    private final String descricao;

    MaterialFlange(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
