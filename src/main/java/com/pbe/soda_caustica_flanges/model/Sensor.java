package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sensor_monitoramento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sensor_codigo_identificacao",
                columnNames = "codigo_identificacao"
        )
)
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_identificacao", nullable = false, length = 60)
    private String codigoIdentificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoSensor tipo;

    @Column(length = 100)
    private String fabricante;

    @Column(length = 100)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flange_id", nullable = false)
    private Flange flange;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusSensor status = StatusSensor.CADASTRADO;

    @Column(name = "data_instalacao")
    private LocalDate dataInstalacao;

    @Column(name = "ultima_calibracao")
    private LocalDate ultimaCalibracao;

    @Column(name = "proxima_calibracao")
    private LocalDate proximaCalibracao;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void antesDeCriar() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    public void antesDeAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public boolean isCalibracaoVencida() {
        return proximaCalibracao != null
                && proximaCalibracao.isBefore(LocalDate.now())
                && getStatus() != StatusSensor.DESATIVADO;
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

    public Flange getFlange() { return flange; }
    public void setFlange(Flange flange) { this.flange = flange; }

    public StatusSensor getStatus() {
        return status == null ? StatusSensor.CADASTRADO : status;
    }
    public void setStatus(StatusSensor status) { this.status = status; }

    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public void setDataInstalacao(LocalDate dataInstalacao) { this.dataInstalacao = dataInstalacao; }

    public LocalDate getUltimaCalibracao() { return ultimaCalibracao; }
    public void setUltimaCalibracao(LocalDate ultimaCalibracao) { this.ultimaCalibracao = ultimaCalibracao; }

    public LocalDate getProximaCalibracao() { return proximaCalibracao; }
    public void setProximaCalibracao(LocalDate proximaCalibracao) { this.proximaCalibracao = proximaCalibracao; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
