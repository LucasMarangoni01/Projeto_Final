package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da unidade é obrigatório.")
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "A localização da unidade é obrigatória.")
    @Column(nullable = false, length = 150)
    private String localizacao;

    @Column(nullable = false)
    private boolean ativo = true;

    public Unidade() {
    }

    public Unidade(String nome, String localizacao, boolean ativo) {
        this.nome = nome;
        this.localizacao = localizacao;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}