package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Role;
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

import java.util.Locale;

@Configuration
public class SecurityConfig {

    private final UsuarioRepository repository;
    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(
            UsuarioRepository repository,
            LoginSuccessHandler loginSuccessHandler) {
        this.repository = repository;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

            Usuario usuario = repository.findByEmail(emailNormalizado)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "Usuário não encontrado: " + email
                            )
                    );

            boolean usuarioSemUnidade =
                    usuario.getRole() == Role.USER
                            && usuario.getUnidade() == null;

            boolean usuarioDesabilitado =
                    !usuario.isAtivo()
                            || usuarioSemUnidade;

            return User.builder()
                    .username(usuario.getEmail())
                    .password(usuario.getSenha())
                    .roles(usuario.getRole().name())
                    .disabled(usuarioDesabilitado)
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/cadastro",
                                "/error",
                                "/login.css",
                                "/cadastro.css",
                                "/style.css",
                                "/menu.css",
                                "/menu.js",
                                "/perfil.css",
                                "/unidade.css",
                                "/flange.css",
                                "/js/**",
                                "/css/**",
                                "/img/**",
                                "/foto_ocorrencia/**",
                                "/foto_pessoa/**",
                                "/foto_flange/**"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/flange/**",
                                "/funcionario/**",
                                "/manutencao/**",
                                "/incidente/**",
                                "/perfil"
                        ).hasAnyRole("ADMIN", "USER")
                        .requestMatchers(
                                "/usuarios/**",
                                "/usuario/**",
                                "/unidades/**"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .successHandler(loginSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}
