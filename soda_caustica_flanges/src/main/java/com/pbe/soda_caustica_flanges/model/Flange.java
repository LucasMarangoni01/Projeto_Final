package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "flange")
public class Flange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome_flange;

    @NotBlank
    private String localizacao;

    @NotNull
    private Double temp_ambiente;

    // ==========================================
    // CORREÇÃO: Conversão de data para não falhar a validação
    // ==========================================
    @NotNull(message = "A data de entrada é obrigatória.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate data_entrada;

    private String foto;

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome_flange() {
        return nome_flange;
    }

    public void setNome_flange(String nome_flange) {
        this.nome_flange = nome_flange;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public Double getTemp_ambiente() {
        return temp_ambiente;
    }

    public void setTemp_ambiente(Double temp_ambiente) {
        this.temp_ambiente = temp_ambiente;
    }

    public LocalDate getData_entrada() {
        return data_entrada;
    }

    public void setData_entrada(LocalDate data_entrada) {
        this.data_entrada = data_entrada;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}