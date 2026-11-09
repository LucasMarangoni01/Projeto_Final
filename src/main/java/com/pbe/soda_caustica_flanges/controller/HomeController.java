package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/")
public class HomeController {

    private final FlangeRepository flangeRepository;
    private final UsuarioRepository usuarioRepository;

    public HomeController(
            FlangeRepository flangeRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.flangeRepository = flangeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==============================
    // Página Inicial (localhost/)
    // ==============================
    @GetMapping
    public String home(
            Model model,
            Authentication authentication
    ) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow();

        if (usuario.getRole() == Role.ADMIN) {
            model.addAttribute("flanges", flangeRepository.findAll());
        } else if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            model.addAttribute("flanges", Collections.emptyList());
        } else {
            model.addAttribute(
                    "flanges",
                    flangeRepository.findByUnidadeId(usuario.getUnidade().getId())
            );
        }

        return "home/index";
    }
}
