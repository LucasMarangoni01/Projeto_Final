package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome_funcionario;

    // Cada funcionario deve TER UMA PROPRIA FLANGE
    private String funcionario_flange;

    private Integer idade;

    // ==========================================
    // CORREÇÃO: Anotações de data aplicadas AQUI
    // ==========================================
    @NotNull(message = "A data de nascimento é obrigatória.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate data_nascimento;

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public LocalDate getData_nascimento() {
        return data_nascimento;
    }

    public void setData_nascimento(LocalDate data_nascimento) {
        this.data_nascimento = data_nascimento;
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public String getFuncionario_flange() {
        return funcionario_flange;
    }

    public void setFuncionario_flange(String funcionario_flange) {
        this.funcionario_flange = funcionario_flange;
    }

    public String getNome_funcionario() {
        return nome_funcionario;
    }

    public void setNome_funcionario(String nome_funcionario) {
        this.nome_funcionario = nome_funcionario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}