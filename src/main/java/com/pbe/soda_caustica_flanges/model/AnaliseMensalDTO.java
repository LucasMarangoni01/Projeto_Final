package com.pbe.soda_caustica_flanges.model;

/**
 * Dados agregados de incidentes e manutenções para um mês do período analisado.
 */
public class AnaliseMensalDTO {

    private final String mes;
    private final long incidentes;
    private final long manutencoes;
    private final int incidentesPercentual;
    private final int manutencoesPercentual;

    public AnaliseMensalDTO(
            String mes,
            long incidentes,
            long manutencoes,
            int incidentesPercentual,
            int manutencoesPercentual) {
        this.mes = mes;
        this.incidentes = incidentes;
        this.manutencoes = manutencoes;
        this.incidentesPercentual = incidentesPercentual;
        this.manutencoesPercentual = manutencoesPercentual;
    }

    public String getMes() { return mes; }
    public long getIncidentes() { return incidentes; }
    public long getManutencoes() { return manutencoes; }
    public int getIncidentesPercentual() { return incidentesPercentual; }
    public int getManutencoesPercentual() { return manutencoesPercentual; }
}
