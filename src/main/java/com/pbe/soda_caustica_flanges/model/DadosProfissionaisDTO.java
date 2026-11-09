package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.Size;

public class DadosProfissionaisDTO {

    @Size(max = 30, message = "A matrícula deve ter no máximo 30 caracteres.")
    private String matricula;

    @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres.")
    private String cargo;

    @Size(max = 100, message = "O setor deve ter no máximo 100 caracteres.")
    private String setor;

    public DadosProfissionaisDTO() {
    }

    public DadosProfissionaisDTO(Usuario usuario) {
        this.matricula = usuario.getMatricula();
        this.cargo = usuario.getCargo();
        this.setor = usuario.getSetor();
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}
