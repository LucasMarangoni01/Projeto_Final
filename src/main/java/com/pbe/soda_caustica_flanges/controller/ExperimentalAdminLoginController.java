package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import com.pbe.soda_caustica_flanges.config.LoginSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * Login instantâneo ADMIN exclusivamente para testes locais.
 *
 * Requer simultaneamente:
 * - execução em localhost/loopback;
 * - app.experimental.admin-shortcut.enabled=true (padrão experimental);
 * - requisição originada de loopback e host local;
 * - conta ADMIN ativa, única ou explicitamente configurada.
 *
 * Esta rota não deve ser habilitada em produção.
 */
@Controller
public class ExperimentalAdminLoginController {

    private static final String AUDIT_REQUEST_ATTRIBUTE = "experimentalAdminShortcut";

    private final UsuarioRepository usuarioRepository;
    private final UserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final Environment environment;
    private final boolean shortcutEnabled;
    private final String configuredAdminEmail;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public ExperimentalAdminLoginController(
            UsuarioRepository usuarioRepository,
            UserDetailsService userDetailsService,
            LoginSuccessHandler loginSuccessHandler,
            Environment environment,
            @Value("${app.experimental.admin-shortcut.enabled:true}") boolean shortcutEnabled,
            @Value("${app.experimental.admin-shortcut.email:}") String configuredAdminEmail) {
        this.usuarioRepository = usuarioRepository;
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.environment = environment;
        this.shortcutEnabled = shortcutEnabled;
        this.configuredAdminEmail = configuredAdminEmail;
    }

    @PostMapping("/experimental/admin-login")
    public void entrarComoAdmin(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, jakarta.servlet.ServletException {

        if (!atalhoPermitido(request)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Usuario admin = localizarAdmin();
        if (admin == null) {
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/login?adminShortcutError=account");
            return;
        }

        if (admin.getRole() != Role.ADMIN || !admin.isAtivo()) {
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/login?adminShortcutError=inactive");
            return;
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(admin.getEmail());
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/login?adminShortcutError=account");
            return;
        }

        if (!userDetails.isEnabled()
                || userDetails.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            SecurityContextHolder.clearContext();
            response.sendRedirect(request.getContextPath() + "/login?adminShortcutError=inactive");
            return;
        }

        // Renova o identificador de sessão antes de instalar a autenticação.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        request.setAttribute(AUDIT_REQUEST_ATTRIBUTE, Boolean.TRUE);
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
    }

    private boolean atalhoPermitido(HttpServletRequest request) {
        if (!shortcutEnabled) {
            return false;
        }

        String host = request.getServerName();
        if (host == null) {
            return false;
        }

        String hostNormalizado = host.toLowerCase(Locale.ROOT);
        boolean hostLocal = "localhost".equals(hostNormalizado)
                || "127.0.0.1".equals(hostNormalizado)
                || "::1".equals(hostNormalizado)
                || "[::1]".equals(hostNormalizado);

        if (!hostLocal) {
            return false;
        }

        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }

        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private Usuario localizarAdmin() {
        if (configuredAdminEmail != null && !configuredAdminEmail.isBlank()) {
            return usuarioRepository.findByEmail(
                    configuredAdminEmail.trim().toLowerCase(Locale.ROOT)
            ).orElse(null);
        }

        List<Usuario> adminsAtivos =
                usuarioRepository.findByRoleAndAtivoTrueOrderByNomeAsc(Role.ADMIN);

        // Não escolhe uma conta arbitrariamente quando há vários administradores.
        return adminsAtivos.size() == 1 ? adminsAtivos.get(0) : null;
    }
}
