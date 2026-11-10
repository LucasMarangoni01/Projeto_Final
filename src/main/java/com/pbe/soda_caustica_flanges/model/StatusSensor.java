package com.pbe.soda_caustica_flanges.model;

public enum StatusSensor {

    CADASTRADO("Cadastrado", "sensor-status-registered"),
    INSTALADO("Instalado", "sensor-status-installed"),
    EM_MANUTENCAO("Em manutenção", "sensor-status-maintenance"),
    DESATIVADO("Desativado", "sensor-status-disabled");

    private final String descricao;
    private final String classeCss;

    StatusSensor(String descricao, String classeCss) {
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
