package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PerfilDTO {

    @NotBlank(message = "Informe seu nome completo.")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
    private String nome;

    @Size(max = 15, message = "O telefone deve ter no máximo 15 caracteres.")
    @Pattern(
            regexp = "^(?:|\\d{10,11}|\\(\\d{2}\\) \\d{4,5}-\\d{4})$",
            message = "Informe um telefone com DDD no formato (11) 99999-9999."
    )
    private String telefone;

    public PerfilDTO() {
    }

    public PerfilDTO(Usuario usuario) {
        this.nome = usuario.getNome();
        this.telefone = usuario.getTelefone();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
