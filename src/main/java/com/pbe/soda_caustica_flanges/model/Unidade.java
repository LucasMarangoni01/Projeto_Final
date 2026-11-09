package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da unidade é obrigatório.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "A localização da unidade é obrigatória.")
    @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres.")
    @Column(nullable = false, length = 150)
    private String localizacao;

    @Size(max = 30, message = "O código interno deve ter no máximo 30 caracteres.")
    @Column(name = "codigo_interno", unique = true, length = 30)
    private String codigo;

    @Size(max = 100, message = "O nome do responsável deve ter no máximo 100 caracteres.")
    @Column(length = 100)
    private String responsavel;

    @Size(max = 25, message = "O telefone deve ter no máximo 25 caracteres.")
    @Column(name = "telefone_contato", length = 25)
    private String telefoneContato;

    @Email(message = "Informe um e-mail de contato válido.")
    @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
    @Column(name = "email_contato", length = 120)
    private String emailContato;

    @Size(max = 600, message = "A descrição deve ter no máximo 600 caracteres.")
    @Column(length = 600)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Unidade() {
    }

    public Unidade(String nome, String localizacao, boolean ativo) {
        this.nome = nome;
        this.localizacao = localizacao;
        this.ativo = ativo;
    }

    @PrePersist
    public void antesDeCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    public void antesDeAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public String getTelefoneContato() { return telefoneContato; }
    public void setTelefoneContato(String telefoneContato) { this.telefoneContato = telefoneContato; }

    public String getEmailContato() { return emailContato; }
    public void setEmailContato(String emailContato) { this.emailContato = emailContato; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
