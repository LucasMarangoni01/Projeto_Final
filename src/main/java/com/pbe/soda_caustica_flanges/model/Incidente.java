package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.Month;

@Entity
@Table(name = "incidentes",
        indexes = {
                @Index(name = "idx_incidente_flange_data", columnList = "flange_id, data_hora"),
                @Index(name = "idx_incidente_status_alarme", columnList = "status_alarme, normalizado_em")
        })
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campos históricos mantidos para compatibilidade com os registros agregados antigos.
    @Column(nullable = false)
    private Integer ano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Month mes;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private String localizacao;

    // Dados de ocorrência individual originada por telemetria. São nulos em registros legados.
    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flange_id")
    private Flange flange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leitura_sensor_id")
    private LeituraSensor leituraSensor;

    @Column(name = "tipo_ocorrencia", length = 80)
    private String tipoOcorrencia;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status_alarme", length = 30)
    private StatusAlarme statusAlarme;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_flange_anterior", length = 30)
    private StatusFlange statusFlangeAnterior;

    @Column(name = "reconhecido_em")
    private LocalDateTime reconhecidoEm;

    @Column(name = "reconhecido_por_email", length = 120)
    private String reconhecidoPorEmail;

    @Column(name = "justificativa_reconhecimento", length = 500)
    private String justificativaReconhecimento;

    @Column(name = "normalizado_em")
    private LocalDateTime normalizadoEm;

    @Column(name = "normalizado_por_email", length = 120)
    private String normalizadoPorEmail;

    @Column(name = "justificativa_normalizacao", length = 500)
    private String justificativaNormalizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }

    public Month getMes() { return mes; }
    public void setMes(Month mes) { this.mes = mes; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Flange getFlange() { return flange; }
    public void setFlange(Flange flange) { this.flange = flange; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public LeituraSensor getLeituraSensor() { return leituraSensor; }
    public void setLeituraSensor(LeituraSensor leituraSensor) { this.leituraSensor = leituraSensor; }

    public String getTipoOcorrencia() { return tipoOcorrencia; }
    public void setTipoOcorrencia(String tipoOcorrencia) { this.tipoOcorrencia = tipoOcorrencia; }

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

    public StatusAlarme getStatusAlarme() { return statusAlarme; }
    public void setStatusAlarme(StatusAlarme statusAlarme) { this.statusAlarme = statusAlarme; }

    public StatusFlange getStatusFlangeAnterior() { return statusFlangeAnterior; }
    public void setStatusFlangeAnterior(StatusFlange statusFlangeAnterior) {
        this.statusFlangeAnterior = statusFlangeAnterior;
    }

    public LocalDateTime getReconhecidoEm() { return reconhecidoEm; }
    public void setReconhecidoEm(LocalDateTime reconhecidoEm) { this.reconhecidoEm = reconhecidoEm; }

    public String getReconhecidoPorEmail() { return reconhecidoPorEmail; }
    public void setReconhecidoPorEmail(String reconhecidoPorEmail) { this.reconhecidoPorEmail = reconhecidoPorEmail; }

    public String getJustificativaReconhecimento() { return justificativaReconhecimento; }
    public void setJustificativaReconhecimento(String justificativaReconhecimento) { this.justificativaReconhecimento = justificativaReconhecimento; }

    public LocalDateTime getNormalizadoEm() { return normalizadoEm; }
    public void setNormalizadoEm(LocalDateTime normalizadoEm) { this.normalizadoEm = normalizadoEm; }

    public String getNormalizadoPorEmail() { return normalizadoPorEmail; }
    public void setNormalizadoPorEmail(String normalizadoPorEmail) { this.normalizadoPorEmail = normalizadoPorEmail; }

    public String getJustificativaNormalizacao() { return justificativaNormalizacao; }
    public void setJustificativaNormalizacao(String justificativaNormalizacao) { this.justificativaNormalizacao = justificativaNormalizacao; }

    public boolean isAlarmeAtivo() {
        return dataHora != null && normalizadoEm == null;
    }
}
