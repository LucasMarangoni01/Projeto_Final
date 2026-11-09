package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Init implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public Init(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:${app.admin.email:}}") String adminEmail,
            @Value("${ADMIN_PASSWORD:${app.admin.password:}}") String adminPassword
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        boolean emailInformado = adminEmail != null && !adminEmail.isBlank();
        boolean senhaInformada = adminPassword != null && !adminPassword.isBlank();

        if (!emailInformado && !senhaInformada) {
            System.out.println(
                    "Bootstrap ADMIN não configurado. Defina ADMIN_EMAIL e ADMIN_PASSWORD para criar a conta inicial."
            );
            return;
        }

        if (!emailInformado || !senhaInformada) {
            throw new IllegalStateException(
                    "Configure ADMIN_EMAIL e ADMIN_PASSWORD juntos para criar a conta ADMIN inicial."
            );
        }

        if (adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD deve possuir pelo menos 12 caracteres."
            );
        }

        String emailNormalizado = adminEmail.trim().toLowerCase(Locale.ROOT);

        if (repository.findByEmail(emailNormalizado).isPresent()) {
            System.out.println("A conta ADMIN configurada já existe. Nenhuma senha foi alterada.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(emailNormalizado);
        admin.setSenha(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setAtivo(true);

        repository.save(admin);

        System.out.println("Conta ADMIN inicial criada com sucesso.");
    }
}
