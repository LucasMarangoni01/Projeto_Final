package com.pbe.soda_caustica_flanges.model;

public enum ClassePressaoFlange {
    CLASSE_150("150#"),
    CLASSE_300("300#"),
    CLASSE_600("600#");

    private final String descricao;

    ClassePressaoFlange(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
