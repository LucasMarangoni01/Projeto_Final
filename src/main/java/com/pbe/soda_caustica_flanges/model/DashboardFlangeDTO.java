package com.pbe.soda_caustica_flanges.model;

/**
 * Projeção segura de dados reais usados no dashboard.
 * Não expõe entidades JPA, credenciais ou relacionamentos lazy ao navegador.
 */
public record DashboardFlangeDTO(
        int posicao,
        Long id,
        String nomeFlange,
        String localizacao,
        Double temperaturaAmbiente,
        String dataEntrada,
        String status,
        String foto,
        int totalSensores,
        int sensoresInstalados,
        String tiposSensores,
        double mapaX,
        double mapaY,
        Double ultimaTemperatura,
        String ultimaLeituraEm,
        boolean alarmeAtivo) {

    public int getPosicao() { return posicao; }
    public Long getId() { return id; }
    public String getNomeFlange() { return nomeFlange; }
    public String getLocalizacao() { return localizacao; }
    public Double getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public String getDataEntrada() { return dataEntrada; }
    public String getStatus() { return status; }
    public String getFoto() { return foto; }
    public int getTotalSensores() { return totalSensores; }
    public int getSensoresInstalados() { return sensoresInstalados; }
    public String getTiposSensores() { return tiposSensores; }
    public double getMapaX() { return mapaX; }
    public double getMapaY() { return mapaY; }
    public Double getUltimaTemperatura() { return ultimaTemperatura; }
    public String getUltimaLeituraEm() { return ultimaLeituraEm; }
    public boolean isAlarmeAtivo() { return alarmeAtivo; }
}
