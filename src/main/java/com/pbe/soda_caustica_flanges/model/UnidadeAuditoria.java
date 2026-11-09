package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "unidade_auditoria",
        indexes = {
                @Index(name = "idx_unidade_auditoria_unidade", columnList = "unidade_id"),
                @Index(name = "idx_unidade_auditoria_data", columnList = "data_evento")
        }
)
public class UnidadeAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    @Column(name = "unidade_nome", nullable = false, length = 100)
    private String unidadeNome;

    @Column(name = "ator_email", nullable = false, length = 100)
    private String atorEmail;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(length = 600)
    private String detalhes;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;

    @PrePersist
    public void antesDeCriar() {
        if (dataEvento == null) {
            dataEvento = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Long getUnidadeId() { return unidadeId; }
    public void setUnidadeId(Long unidadeId) { this.unidadeId = unidadeId; }

    public String getUnidadeNome() { return unidadeNome; }
    public void setUnidadeNome(String unidadeNome) { this.unidadeNome = unidadeNome; }

    public String getAtorEmail() { return atorEmail; }
    public void setAtorEmail(String atorEmail) { this.atorEmail = atorEmail; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public LocalDateTime getDataEvento() { return dataEvento; }
}
