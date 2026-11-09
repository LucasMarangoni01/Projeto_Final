package com.pbe.soda_caustica_flanges.model;

public record UnidadeResumoDTO(
        Unidade unidade,
        long totalUsuarios,
        long usuariosAtivos,
        long totalFuncionarios,
        long totalFlanges,
        long totalManutencoes) {
}
