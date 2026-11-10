package com.pbe.soda_caustica_flanges.model;

public enum StatusAtividadeManutencao {

    REGISTRADA("Registrada", "activity-registered"),
    AGUARDANDO_MATERIAL("Aguardando material", "activity-waiting"),
    EM_ANDAMENTO("Em andamento", "activity-progress"),
    CONCLUIDA("Concluída", "activity-completed"),
    SUSPENSA("Suspensa", "activity-paused");

    private final String descricao;
    private final String classeCss;

    StatusAtividadeManutencao(String descricao, String classeCss) {
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
