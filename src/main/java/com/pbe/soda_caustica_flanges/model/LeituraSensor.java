package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leitura_sensor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_leitura_codigo_mensagem",
                columnNames = "codigo_mensagem"
        ),
        indexes = {
                @Index(name = "idx_leitura_sensor_data", columnList = "sensor_id, recebida_em"),
                @Index(name = "idx_leitura_flange_data", columnList = "flange_id, recebida_em")
        })
public class LeituraSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flange_id", nullable = false)
    private Flange flange;

    @Column(name = "recebida_em", nullable = false)
    private LocalDateTime recebidaEm;

    @Column(name = "codigo_mensagem", nullable = false, length = 80)
    private String codigoMensagem;

    @Column(name = "temperatura_celsius")
    private Double temperatura;

    @Column(name = "ph")
    private Double ph;

    @Column(name = "condutividade")
    private Double condutividade;

    @Column(name = "pressao_diferencial")
    private Double pressaoDiferencial;

    @Column(name = "vazamento_detectado")
    private Boolean vazamentoDetectado;

    @Column(name = "alarme_disparado", nullable = false)
    private boolean alarmeDisparado;

    @Column(name = "origem", length = 30, nullable = false)
    private String origem = "HARDWARE";

    @PrePersist
    public void antesDeCriar() {
        if (recebidaEm == null) {
            recebidaEm = LocalDateTime.now();
        }
        if (origem == null || origem.isBlank()) {
            origem = "HARDWARE";
        }
    }

    public Long getId() { return id; }
    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }
    public Flange getFlange() { return flange; }
    public void setFlange(Flange flange) { this.flange = flange; }
    public LocalDateTime getRecebidaEm() { return recebidaEm; }
    public void setRecebidaEm(LocalDateTime recebidaEm) { this.recebidaEm = recebidaEm; }
    public String getCodigoMensagem() { return codigoMensagem; }
    public void setCodigoMensagem(String codigoMensagem) { this.codigoMensagem = codigoMensagem; }
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
    public boolean isAlarmeDisparado() { return alarmeDisparado; }
    public void setAlarmeDisparado(boolean alarmeDisparado) { this.alarmeDisparado = alarmeDisparado; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
}
