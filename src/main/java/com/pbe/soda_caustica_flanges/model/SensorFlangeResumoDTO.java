package com.pbe.soda_caustica_flanges.model;

import java.util.List;

public class SensorFlangeResumoDTO {

    private final Long flangeId;
    private final String nomeFlange;
    private final String unidade;
    private final long totalSensores;
    private final long instalados;
    private final List<TipoSensor> tiposSensores;

    public SensorFlangeResumoDTO(
            Long flangeId,
            String nomeFlange,
            String unidade,
            long totalSensores,
            long instalados,
            List<TipoSensor> tiposSensores) {
        this.flangeId = flangeId;
        this.nomeFlange = nomeFlange;
        this.unidade = unidade;
        this.totalSensores = totalSensores;
        this.instalados = instalados;
        this.tiposSensores = tiposSensores;
    }

    public Long getFlangeId() { return flangeId; }
    public String getNomeFlange() { return nomeFlange; }
    public String getUnidade() { return unidade; }
    public long getTotalSensores() { return totalSensores; }
    public long getInstalados() { return instalados; }
    public List<TipoSensor> getTiposSensores() { return tiposSensores; }
}
