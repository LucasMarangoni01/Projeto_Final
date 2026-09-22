package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.UnidadeService;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UnidadeService unidadeService;

    public UsuarioController(
            UsuarioService usuarioService,
            UnidadeService unidadeService) {

        this.usuarioService = usuarioService;
        this.unidadeService = unidadeService;
    }

    @GetMapping
    public String listar(
            Model model,
            @RequestParam(required = false) String erro) {

        model.addAttribute(
                "usuarios",
                usuarioService.listarTodos()
        );

        model.addAttribute("erro", erro);

        return "usuario/listagem";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model) {

        Usuario usuario =
                usuarioService.buscarPorId(id);

        model.addAttribute(
                "usuario",
                usuario
        );

        model.addAttribute(
                "roles",
                Role.values()
        );

        model.addAttribute(
                "unidades",
                unidadeService.listarAtivas()
        );

        return "usuario/editar";
    }

    @PostMapping("/alterar-role")
    public String alterarRole(
            @RequestParam Long id,
            @RequestParam Role role,
            Authentication authentication) {

        try {
            usuarioService.alterarRole(
                    id,
                    role,
                    authentication.getName()
            );

            return "redirect:/usuarios";

        } catch (IllegalArgumentException e) {
            return "redirect:/usuarios?erro=role";
        }
    }

    @PostMapping("/alterar-unidade")
    public String alterarUnidade(
            @RequestParam Long id,
            @RequestParam Long unidadeId) {

        try {
            Unidade unidade =
                    unidadeService.buscarPorId(unidadeId);

            if (!unidade.isAtivo()) {
                return "redirect:/usuarios?erro=unidade";
            }

            usuarioService.alterarUnidade(
                    id,
                    unidade
            );

            return "redirect:/usuarios";

        } catch (IllegalArgumentException e) {
            return "redirect:/usuarios?erro=unidade";
        }
    }

    @PostMapping("/alterar-status")
    public String alterarStatus(
            @RequestParam Long id,
            Authentication authentication) {

        try {
            usuarioService.alterarStatus(
                    id,
                    authentication.getName()
            );

            return "redirect:/usuarios";

        } catch (IllegalArgumentException e) {
            return "redirect:/usuarios?erro=status";
        }
    }
}