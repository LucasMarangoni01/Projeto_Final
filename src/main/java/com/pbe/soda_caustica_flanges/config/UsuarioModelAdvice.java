package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UsuarioModelAdvice {
    private final UsuarioService usuarioService;

    public UsuarioModelAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("usuarioLogado")
    public Usuario usuarioLogado(Authentication authentication) {
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return usuarioService.buscarPorEmail(authentication.getName());
    }
}