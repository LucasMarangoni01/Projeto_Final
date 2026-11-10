package com.pbe.soda_caustica_flanges.model;

public enum NivelRiscoManutencao {

    BAIXO("Baixo", "risk-low"),
    MODERADO("Moderado", "risk-medium"),
    ALTO("Alto", "risk-high"),
    CRITICO("Crítico", "risk-critical");

    private final String descricao;
    private final String classeCss;

    NivelRiscoManutencao(String descricao, String classeCss) {
        this.descricao = descricao;
        this.classeCss = classeCss;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getClasseCss() {
        return classeCss;
    }
}
