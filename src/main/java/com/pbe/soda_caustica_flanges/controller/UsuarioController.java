package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "usuarios",
                usuarioService.listarTodos()
        );

        return "usuario/listagem";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id,
                         Model model) {

        Usuario usuario = usuarioService.buscarPorId(id);

        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", Role.values());

        return "usuario/editar";
    }

    @PostMapping("/alterar-role")
    public String alterarRole(
            @RequestParam Long id,
            @RequestParam Role role) {

        usuarioService.alterarRole(id, role);

        return "redirect:/usuarios";
    }

    @PostMapping("/alterar-status")
    public String alterarStatus(
            @RequestParam Long id) {

        usuarioService.alterarStatus(id);

        return "redirect:/usuarios";
    }
}