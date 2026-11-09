package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.UnidadeService;
import com.pbe.soda_caustica_flanges.service.UsuarioAuditoriaService;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UnidadeService unidadeService;
    private final UsuarioAuditoriaService auditoriaService;

    public UsuarioController(
            UsuarioService usuarioService,
            UnidadeService unidadeService,
            UsuarioAuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.unidadeService = unidadeService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(
            Model model,
            @RequestParam(required = false) String erro,
            @RequestParam(required = false) String sucesso) {

        List<Usuario> usuarios = usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("totalAtivos", usuarios.stream().filter(Usuario::isAtivo).count());
        model.addAttribute("totalAdministradores", usuarios.stream()
                .filter(usuario -> usuario.getRole() == Role.ADMIN && usuario.isAtivo())
                .count());
        model.addAttribute("contasSemUnidade", usuarios.stream()
                .filter(usuario -> usuario.getRole() == Role.USER && usuario.getUnidade() == null)
                .count());
        model.addAttribute("erro", erro);
        model.addAttribute("sucesso", sucesso);
        return "usuario/listagem";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);

        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", Role.values());
        model.addAttribute("unidades", unidadeService.listarAtivas());
        model.addAttribute(
                "historicoUsuario",
                auditoriaService.buscarPorUsuario(usuario.getId())
        );

        return "usuario/editar";
    }

    @PostMapping("/alterar-role")
    public String alterarRole(
            @RequestParam Long id,
            @RequestParam Role role,
            Authentication authentication) {
        try {
            usuarioService.alterarRole(id, role, authentication.getName());
            return "redirect:/usuarios?sucesso=role";
        } catch (IllegalArgumentException e) {
            return "redirect:/usuarios?erro=role";
        }
    }

    @PostMapping("/alterar-unidade")
    public String alterarUnidade(
            @RequestParam Long id,
            @RequestParam Long unidadeId,
            Authentication authentication) {
        try {
            Unidade unidade = unidadeService.buscarPorId(unidadeId);
            if (!unidade.isAtivo()) {
                return "redirect:/usuarios?erro=unidade";
            }

            usuarioService.alterarUnidade(id, unidade, authentication.getName());
            return "redirect:/usuarios?sucesso=unidade";
        } catch (IllegalArgumentException e) {
            return "redirect:/usuarios?erro=unidade";
        }
    }

    @PostMapping("/alterar-status")
    public String alterarStatus(
            @RequestParam Long id,
            Authentication authentication) {
        try {
            usuarioService.alterarStatus(id, authentication.getName());
            return "redirect:/usuarios?sucesso=status";
        } catch (IllegalArgumentException e) {
            return "redirect:/usuarios?erro=status";
        }
    }
}
