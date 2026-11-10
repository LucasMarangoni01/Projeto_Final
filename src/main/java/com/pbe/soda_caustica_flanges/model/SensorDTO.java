package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class SensorDTO {

    private Long id;

    @NotBlank(message = "Informe o código de identificação do sensor.")
    @Size(max = 60, message = "O código deve ter no máximo 60 caracteres.")
    private String codigoIdentificacao;

    @NotNull(message = "Selecione o tipo de sensor.")
    private TipoSensor tipo;

    @Size(max = 100, message = "O fabricante deve ter no máximo 100 caracteres.")
    private String fabricante;

    @Size(max = 100, message = "O modelo deve ter no máximo 100 caracteres.")
    private String modelo;

    @Size(max = 100, message = "O número de série deve ter no máximo 100 caracteres.")
    private String numeroSerie;

    @NotNull(message = "Selecione a flange que receberá este sensor.")
    private Long flangeId;

    @NotNull(message = "Selecione o status do sensor.")
    private StatusSensor status = StatusSensor.CADASTRADO;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataInstalacao;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate ultimaCalibracao;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate proximaCalibracao;

    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
    private String observacoes;

    public SensorDTO() {
    }

    public SensorDTO(Sensor sensor) {
        this.id = sensor.getId();
        this.codigoIdentificacao = sensor.getCodigoIdentificacao();
        this.tipo = sensor.getTipo();
        this.fabricante = sensor.getFabricante();
        this.modelo = sensor.getModelo();
        this.numeroSerie = sensor.getNumeroSerie();
        this.flangeId = sensor.getFlange() == null ? null : sensor.getFlange().getId();
        this.status = sensor.getStatus();
        this.dataInstalacao = sensor.getDataInstalacao();
        this.ultimaCalibracao = sensor.getUltimaCalibracao();
        this.proximaCalibracao = sensor.getProximaCalibracao();
        this.observacoes = sensor.getObservacoes();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoIdentificacao() { return codigoIdentificacao; }
    public void setCodigoIdentificacao(String codigoIdentificacao) { this.codigoIdentificacao = codigoIdentificacao; }

    public TipoSensor getTipo() { return tipo; }
    public void setTipo(TipoSensor tipo) { this.tipo = tipo; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }

    public Long getFlangeId() { return flangeId; }
    public void setFlangeId(Long flangeId) { this.flangeId = flangeId; }

    public StatusSensor getStatus() { return status; }
    public void setStatus(StatusSensor status) { this.status = status; }

    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public void setDataInstalacao(LocalDate dataInstalacao) { this.dataInstalacao = dataInstalacao; }

    public LocalDate getUltimaCalibracao() { return ultimaCalibracao; }
    public void setUltimaCalibracao(LocalDate ultimaCalibracao) { this.ultimaCalibracao = ultimaCalibracao; }

    public LocalDate getProximaCalibracao() { return proximaCalibracao; }
    public void setProximaCalibracao(LocalDate proximaCalibracao) { this.proximaCalibracao = proximaCalibracao; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
