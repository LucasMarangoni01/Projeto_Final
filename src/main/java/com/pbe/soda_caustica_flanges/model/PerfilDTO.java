package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PerfilDTO {

    @NotBlank(message = "Informe seu nome completo.")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
    private String nome;

    @Size(max = 25, message = "O telefone deve ter no máximo 25 caracteres.")
    @Pattern(regexp = "^[0-9+() .-]*$", message = "Informe um telefone válido.")
    private String telefone;

    @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres.")
    private String cargo;

    @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres.")
    private String setor;

    @Size(max = 30, message = "A matrícula deve ter no máximo 30 caracteres.")
    private String matricula;

    public PerfilDTO() {
    }

    public PerfilDTO(Usuario usuario) {
        this.nome = usuario.getNome();
        this.telefone = usuario.getTelefone();
        this.cargo = usuario.getCargo();
        this.setor = usuario.getSetor();
        this.matricula = usuario.getMatricula();
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
}
