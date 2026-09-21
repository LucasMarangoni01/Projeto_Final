package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Init implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public Init(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder
    ) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // Verifica se o administrador já existe
        if (repository.findByEmail("admin@admin.com").isEmpty()) {

            Usuario admin = new Usuario();

            admin.setEmail("admin@admin.com");

            admin.setSenha(
                    passwordEncoder.encode("123")
            );

            admin.setRole(Role.ADMIN);

            admin.setAtivo(true);

            repository.save(admin);

            System.out.println("=================================");
            System.out.println("USUÁRIO ADMIN CRIADO");
            System.out.println("E-mail: admin@admin.com");
            System.out.println("Senha: 123");
            System.out.println("Função: ADMIN");
            System.out.println("=================================");

        } else {

            System.out.println("=================================");
            System.out.println("USUÁRIO ADMIN JÁ EXISTE");
            System.out.println("=================================");

        }
    }
}