package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import com.pbe.soda_caustica_flanges.service.UsuarioAuditoriaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAuditoriaService auditoriaService;

    public LoginSuccessHandler(
            UsuarioRepository usuarioRepository,
            UsuarioAuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            usuario.setUltimoAcesso(LocalDateTime.now());
            usuarioRepository.save(usuario);
            auditoriaService.registrar(
                    usuario,
                    usuario.getEmail(),
                    "LOGIN_REALIZADO",
                    "Acesso ao sistema realizado com sucesso."
            );
        });

        response.sendRedirect(request.getContextPath() + "/");
    }
}
