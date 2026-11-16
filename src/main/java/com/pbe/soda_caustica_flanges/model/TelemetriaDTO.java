package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TelemetriaDTO {

    @NotBlank(message = "O código do sensor é obrigatório.")
    @Size(max = 60)
    private String codigoSensor;

    @DecimalMin(value = "-100.0")
    @DecimalMax(value = "400.0")
    private Double temperatura;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "14.0")
    private Double ph;

    @DecimalMin(value = "0.0")
    private Double condutividade;

    @DecimalMin(value = "0.0")
    private Double pressaoDiferencial;

    private Boolean vazamentoDetectado;

    public String getCodigoSensor() { return codigoSensor; }
    public void setCodigoSensor(String codigoSensor) { this.codigoSensor = codigoSensor; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public Double getPh() { return ph; }
    public void setPh(Double ph) { this.ph = ph; }

    public Double getCondutividade() { return condutividade; }
    public void setCondutividade(Double condutividade) { this.condutividade = condutividade; }

    public Double getPressaoDiferencial() { return pressaoDiferencial; }
    public void setPressaoDiferencial(Double pressaoDiferencial) { this.pressaoDiferencial = pressaoDiferencial; }

    public Boolean getVazamentoDetectado() { return vazamentoDetectado; }
    public void setVazamentoDetectado(Boolean vazamentoDetectado) { this.vazamentoDetectado = vazamentoDetectado; }
}
