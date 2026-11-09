package com.pbe.soda_caustica_flanges.model;

public record UnidadeResumoDTO(
        Unidade unidade,
        long totalUsuarios,
        long usuariosAtivos,
        long totalFuncionarios,
        long totalFlanges,
        long totalManutencoes,
        String criadoPorNome,
        String criadoPorEmail) {

    // Getters JavaBean para compatibilidade com a resolução de propriedades do Thymeleaf.
    public String getCriadoPorNome() {
        return criadoPorNome;
    }

    public String getCriadoPorEmail() {
        return criadoPorEmail;
    }
}
