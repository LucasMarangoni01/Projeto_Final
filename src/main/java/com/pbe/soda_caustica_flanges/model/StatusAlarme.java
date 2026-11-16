package com.pbe.soda_caustica_flanges.model;

public enum StatusAlarme {
    DISPARADO("Disparado"),
    RECONHECIDO("Reconhecido"),
    EM_MANUTENCAO("Em manutenção"),
    NORMALIZADO("Normalizado");

    private final String descricao;

    StatusAlarme(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
