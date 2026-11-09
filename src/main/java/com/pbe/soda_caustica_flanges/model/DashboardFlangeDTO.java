package com.pbe.soda_caustica_flanges.model;

/**
 * Dados simples e seguros usados exclusivamente no dashboard.
 * Não expõe entidades JPA nem relacionamentos lazy ao JavaScript do navegador.
 */
public record DashboardFlangeDTO(
        int posicao,
        Long id,
        String nomeFlange,
        String localizacao,
        Double temperaturaAmbiente,
        String dataEntrada,
        String status,
        String foto) {
}
