package com.pbe.soda_caustica_flanges.model;

/**
 * Dados simples e seguros usados exclusivamente no dashboard.
 * Não expõe entidades JPA nem relacionamentos lazy ao JavaScript do navegador.
 *
 * Os métodos get... mantêm compatibilidade com o acesso a propriedades
 * do Thymeleaf (SpringEL) e com serializadores JavaBean.
 * Os métodos sem prefixo get preservam a API de record já utilizada no controller.
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
        String tiposSensores) {

    public int getPosicao() {
        return posicao;
    }

    public Long getId() {
        return id;
    }

    public String getNomeFlange() {
        return nomeFlange;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public Double getTemperaturaAmbiente() {
        return temperaturaAmbiente;
    }

    public String getDataEntrada() {
        return dataEntrada;
    }

    public String getStatus() {
        return status;
    }

    public String getFoto() {
        return foto;
    }

    public int getTotalSensores() {
        return totalSensores;
    }

    public int getSensoresInstalados() {
        return sensoresInstalados;
    }

    public String getTiposSensores() {
        return tiposSensores;
    }
}
