package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Exercício persistido em tabela própria. Nunca representa telemetria ou incidente real. */
@Entity
@Table(name = "teste_emergencia", indexes = @Index(name = "idx_teste_estado_data", columnList = "estado,detectado_em"))
public class TesteEmergencia {
    public enum Tipo { VAZAMENTO, CRITICO, FALHA_COMUNICACAO }
    public enum Gravidade { MODERADA, ALTA, CRITICA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long versao;
    @Column(name = "flange_id", nullable = false)
    private Long flangeId;
    @Column(nullable = false)
    private String nomeFlange;
    @Column(nullable = false)
    private String localizacao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Tipo tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Gravidade gravidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusAlarme estado;
    @Column(name = "detectado_em", nullable = false)
    private LocalDateTime detectadoEm;
    @Column(nullable = false, length = 120)
    private String iniciadoPor;
    private LocalDateTime reconhecidoEm;
    @Column(length = 120)
    private String reconhecidoPor;
    private LocalDateTime atendimentoEm;
    @Column(length = 120)
    private String atendimentoPor;
    private LocalDateTime encerradoEm;
    @Column(length = 120)
    private String encerradoPor;

    protected TesteEmergencia() {}
    public TesteEmergencia(Flange flange, Tipo tipo, Gravidade gravidade, String ator, LocalDateTime agora) {
        this.flangeId = flange.getId();
        this.nomeFlange = flange.getNomeFlange();
        this.localizacao = flange.getLocalizacao();
        this.tipo = tipo;
        this.gravidade = gravidade;
        this.estado = StatusAlarme.DISPARADO;
        this.detectadoEm = agora;
        this.iniciadoPor = ator;
    }
    public void reconhecer(String ator, LocalDateTime agora) {
        if (!CicloAlarme.aguardaReconhecimento(estado)) throw new IllegalStateException("Teste já reconhecido ou encerrado.");
        estado = StatusAlarme.RECONHECIDO;
        reconhecidoPor = ator;
        reconhecidoEm = agora;
    }
    public void atender(String ator, LocalDateTime agora) {
        if (!CicloAlarme.podeAtender(estado)) throw new IllegalStateException("Reconheça o teste primeiro.");
        estado = StatusAlarme.EM_MANUTENCAO;
        atendimentoPor = ator;
        atendimentoEm = agora;
    }
    public void encerrar(String ator, LocalDateTime agora) {
        if (estado == StatusAlarme.NORMALIZADO) throw new IllegalStateException("Teste já encerrado.");
        // Cancelamento administrativo também é possível, sem reconhecer ou resolver um incidente real.
        estado = StatusAlarme.NORMALIZADO;
        encerradoPor = ator;
        encerradoEm = agora;
    }
    public Long getId() { return id; }
    public Long getFlangeId() { return flangeId; }
    public String getNomeFlange() { return nomeFlange; }
    public String getLocalizacao() { return localizacao; }
    public Tipo getTipo() { return tipo; }
    public Gravidade getGravidade() { return gravidade; }
    public StatusAlarme getEstado() { return estado; }
    public LocalDateTime getDetectadoEm() { return detectadoEm; }
    public String getIniciadoPor() { return iniciadoPor; }
    public LocalDateTime getReconhecidoEm() { return reconhecidoEm; }
    public String getReconhecidoPor() { return reconhecidoPor; }
    public LocalDateTime getAtendimentoEm() { return atendimentoEm; }
    public String getAtendimentoPor() { return atendimentoPor; }
    public LocalDateTime getEncerradoEm() { return encerradoEm; }
    public String getEncerradoPor() { return encerradoPor; }
}
