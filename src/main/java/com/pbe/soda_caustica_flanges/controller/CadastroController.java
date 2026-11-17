package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.CadastroDTO;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestParam;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CadastroController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final boolean codigoInicialObrigatorio;
    private final String codigoInicial;

    public CadastroController(
            UsuarioService usuarioService,
            UsuarioRepository usuarioRepository,
            @Value("${app.security.bootstrap-required:false}") boolean codigoInicialObrigatorio,
            @Value("${app.security.bootstrap-key:}") String codigoInicial
    ) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.codigoInicialObrigatorio = codigoInicialObrigatorio;
        this.codigoInicial = codigoInicial;
    }

    private boolean exigeCodigoInicial() {
        return codigoInicialObrigatorio && usuarioRepository.count() == 0;
    }

    private boolean codigoInicialValido(String informado) {
        return informado != null && !codigoInicial.isBlank()
                && MessageDigest.isEqual(
                    codigoInicial.getBytes(StandardCharsets.UTF_8),
                    informado.getBytes(StandardCharsets.UTF_8));
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

        model.addAttribute("exigirCodigoInstalacao", exigeCodigoInicial());
        return "cadastro/cadastro";
    }

    // ==============================
    // PROCESSAR CADASTRO
    // ==============================
    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid CadastroDTO cadastroDTO,
            BindingResult result,
            @RequestParam(value = "codigoInstalacao", required = false) String codigoInstalacao,
            Model model
    ) {
        boolean primeiroAcesso = exigeCodigoInicial();
        model.addAttribute("exigirCodigoInstalacao", primeiroAcesso);
        if (primeiroAcesso && !codigoInicialValido(codigoInstalacao)) {
            model.addAttribute("erro", "O primeiro cadastro exige o código de instalação configurado pelo responsável.");
            return "cadastro/cadastro";
        }


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
                        .toLowerCase(java.util.Locale.ROOT);


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

        Usuario usuarioCriado = usuarioService.cadastrar(cadastroDTO);

        // O primeiro cadastro recebe ADMIN e já pode acessar o sistema.
        // Os cadastros seguintes são USER e continuam sujeitos à vinculação de unidade.
        if (usuarioCriado.getRole() == Role.ADMIN) {
            return "redirect:/login?cadastro=admin";
        }

        return "redirect:/login?cadastro=pendente";
    }
}