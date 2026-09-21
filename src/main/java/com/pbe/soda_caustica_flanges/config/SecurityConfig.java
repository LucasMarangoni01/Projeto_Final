package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final UsuarioRepository repository;

    public SecurityConfig(UsuarioRepository repository) {
        this.repository = repository;
    }

    // ==============================
    // BUSCAR USUÁRIO NO MYSQL
    // ==============================
    @Bean
    public UserDetailsService userDetailsService() {

        return email -> {

            Usuario usuario = repository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "Usuário não encontrado: " + email
                            )
                    );

            return User.builder()
                    .username(usuario.getEmail())
                    .password(usuario.getSenha())
                    .roles(usuario.getRole().name())
                    .disabled(!usuario.isAtivo())
                    .build();
        };
    }

    // ==============================
    // CRIPTOGRAFIA DA SENHA
    // ==============================
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    // ==============================
    // CONFIGURAÇÃO DE SEGURANÇA
    // ==============================
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // ==============================
                // CSRF
                // ==============================
                .csrf(csrf -> csrf.disable())

                // ==============================
                // PERMISSÕES
                // ==============================
                .authorizeHttpRequests(auth -> auth

                        // ==============================
                        // LOGIN E ARQUIVOS PÚBLICOS
                        // ==============================
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/style.css",
                                "/style2.css",
                                "/js/**",
                                "/img/**",
                                "/foto_ocorrencia/**",
                                "/foto_pessoa/**",
                                "/foto_flange/**"
                        ).permitAll()

                        // ==============================
                        // ADMIN E USER
                        // ==============================
                        .requestMatchers(
                                "/",
                                "/flange/**",
                                "/funcionario/**",
                                "/manutencao/**",
                                "/incidente/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "USER"
                        )

                        // ==============================
                        // SOMENTE ADMIN
                        // ==============================
                        .requestMatchers(
                                "/usuario/**"
                        ).hasRole("ADMIN")

                        // ==============================
                        // QUALQUER OUTRA ROTA
                        // PRECISA DE LOGIN
                        // ==============================
                        .anyRequest().authenticated()
                )

                // ==============================
                // LOGIN
                // ==============================
                .formLogin(form -> form

                        .loginPage("/login")

                        .usernameParameter("email")

                        .passwordParameter("senha")

                        .defaultSuccessUrl("/", true)

                        .failureUrl("/login?error=true")

                        .permitAll()
                )

                // ==============================
                // LOGOUT
                // ==============================
                .logout(logout -> logout

                        .logoutUrl("/logout")

                        .logoutSuccessUrl("/login?logout=true")

                        .permitAll()
                );

        return http.build();
    }
}