package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CadastroDTO {

    @NotBlank(message = "Digite seu nome completo.")
    @Size(
            min = 3,
            max = 100,
            message = "O nome deve ter entre 3 e 100 caracteres."
    )
    private String nome;

    @NotBlank(message = "Digite seu e-mail.")
    @Email(message = "Digite um e-mail válido.")
    private String email;

    @NotBlank(message = "Digite uma senha.")
    @Size(
            min = 6,
            max = 100,
            message = "A senha deve ter entre 6 e 100 caracteres."
    )
    private String senha;

    @NotBlank(message = "Confirme sua senha.")
    private String confirmaSenha;

    public CadastroDTO() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmaSenha() {
        return confirmaSenha;
    }

    public void setConfirmaSenha(String confirmaSenha) {
        this.confirmaSenha = confirmaSenha;
    }
}