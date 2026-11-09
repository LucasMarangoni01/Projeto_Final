package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "manutencao")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ocorrencia;

    private String num_serie;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date data;

    @ManyToOne
    @JoinColumn(name = "flange_id")
    private Flange flange;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @Column(name = "registrado_por_email", length = 100)
    private String registradoPorEmail;

    @Column(name = "registrado_em")
    private LocalDateTime registradoEm;

    @PrePersist
    public void antesDeCriar() {
        if (registradoEm == null) {
            registradoEm = LocalDateTime.now();
        }
    }

    // GETTERS E SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOcorrencia() {
        return ocorrencia;
    }

    public void setOcorrencia(String ocorrencia) {
        this.ocorrencia = ocorrencia;
    }

    public String getNum_serie() {
        return num_serie;
    }

    public void setNum_serie(String num_serie) {
        this.num_serie = num_serie;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public Flange getFlange() {
        return flange;
    }

    public void setFlange(Flange flange) {
        this.flange = flange;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public String getRegistradoPorEmail() {
        return registradoPorEmail;
    }

    public void setRegistradoPorEmail(String registradoPorEmail) {
        this.registradoPorEmail = registradoPorEmail;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }
}