package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "usuario_auditoria",
        indexes = {
                @Index(name = "idx_usuario_auditoria_usuario", columnList = "usuario_id"),
                @Index(name = "idx_usuario_auditoria_data", columnList = "data_evento")
        }
)
public class UsuarioAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID mantido como valor simples para preservar o histórico mesmo se a conta for removida.
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "usuario_email", nullable = false, length = 100)
    private String usuarioEmail;

    @Column(name = "ator_email", nullable = false, length = 100)
    private String atorEmail;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(length = 500)
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
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getAtorEmail() { return atorEmail; }
    public void setAtorEmail(String atorEmail) { this.atorEmail = atorEmail; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public LocalDateTime getDataEvento() { return dataEvento; }
}
