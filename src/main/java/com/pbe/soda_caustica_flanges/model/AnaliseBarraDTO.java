package com.pbe.soda_caustica_flanges.model;

/**
 * Item reutilizável para rankings e distribuições em barras horizontais.
 */
public class AnaliseBarraDTO {

    private final String rotulo;
    private final long quantidade;
    private final int percentual;

    public AnaliseBarraDTO(String rotulo, long quantidade, int percentual) {
        this.rotulo = rotulo;
        this.quantidade = quantidade;
        this.percentual = percentual;
    }

    public String getRotulo() { return rotulo; }
    public long getQuantidade() { return quantidade; }
    public int getPercentual() { return percentual; }
}
