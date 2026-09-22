package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.CadastroDTO;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CadastroController {

    private final UsuarioService usuarioService;

    public CadastroController(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    // ==============================
    // PÁGINA DE CADASTRO
    // ==============================
    @GetMapping("/cadastro")
    public String cadastro(Model model) {

        model.addAttribute(
                "cadastroDTO",
                new CadastroDTO()
        );

        return "cadastro/cadastro";
    }

    // ==============================
    // PROCESSAR CADASTRO
    // ==============================
    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid CadastroDTO cadastroDTO,
            BindingResult result,
            Model model
    ) {

        // ==============================
        // ERROS DE VALIDAÇÃO
        // ==============================

        if (result.hasErrors()) {

            return "cadastro/cadastro";
        }


        // ==============================
        // NORMALIZAR DADOS
        // ==============================

        String nome =
                cadastroDTO.getNome()
                        .trim();

        String email =
                cadastroDTO.getEmail()
                        .trim()
                        .toLowerCase();


        // ==============================
        // VERIFICAR NOME
        // ==============================

        if (nome.isEmpty()) {

            model.addAttribute(
                    "erro",
                    "Digite seu nome completo."
            );

            return "cadastro/cadastro";
        }


        // ==============================
        // VERIFICAR GMAIL
        // ==============================

        if (!email.endsWith("@gmail.com")) {

            model.addAttribute(
                    "erro",
                    "Utilize um endereço de e-mail @gmail.com."
            );

            return "cadastro/cadastro";
        }


        // ==============================
        // ATUALIZAR E-MAIL NORMALIZADO
        // ==============================

        cadastroDTO.setNome(nome);
        cadastroDTO.setEmail(email);


        // ==============================
        // VERIFICAR SENHAS
        // ==============================

        if (!cadastroDTO.getSenha()
                .equals(cadastroDTO.getConfirmaSenha())) {

            model.addAttribute(
                    "erro",
                    "As senhas não são iguais."
            );

            return "cadastro/cadastro";
        }


        // ==============================
        // VERIFICAR E-MAIL EXISTENTE
        // ==============================

        if (usuarioService.emailExiste(email)) {

            model.addAttribute(
                    "erro",
                    "Este e-mail já está cadastrado."
            );

            return "cadastro/cadastro";
        }


        // ==============================
        // SALVAR USUÁRIO
        // ==============================

        usuarioService.cadastrar(
                cadastroDTO
        );


        // ==============================
        // VOLTAR PARA LOGIN
        // ==============================

        return "redirect:/login?cadastro=sucesso";
    }
}