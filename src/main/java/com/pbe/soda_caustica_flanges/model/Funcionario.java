package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do funcionário é obrigatório.")
    @Column(nullable = false)
    private String nome_funcionario;

    // Campo legado de identificação/designação operacional da flange.
    private String funcionario_flange;

    private Integer idade;

    // Opcional para funcionários gerados automaticamente a partir de contas.
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate data_nascimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public LocalDate getData_nascimento() { return data_nascimento; }
    public void setData_nascimento(LocalDate data_nascimento) { this.data_nascimento = data_nascimento; }

    public Integer getIdade() { return idade; }
    public void setIdade(Integer idade) { this.idade = idade; }

    public String getFuncionario_flange() { return funcionario_flange; }
    public void setFuncionario_flange(String funcionario_flange) { this.funcionario_flange = funcionario_flange; }

    public String getNome_funcionario() { return nome_funcionario; }
    public void setNome_funcionario(String nome_funcionario) { this.nome_funcionario = nome_funcionario; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Unidade getUnidade() { return unidade; }
    public void setUnidade(Unidade unidade) { this.unidade = unidade; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
