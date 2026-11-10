package com.pbe.soda_caustica_flanges.config;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

            boolean unidadeInativa =
                    usuario.getUnidade() != null
                            && !usuario.getUnidade().isAtivo();

            boolean usuarioDesabilitado =
                    !usuario.isAtivo()
                            || usuarioSemUnidade
                            || (usuario.getRole() == Role.USER && unidadeInativa);

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
                        // Arquivos estáticos e telas de autenticação.
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
                                "/sensor.css",
                                "/sensor.js",
                                "/analises.css",
                                "/analises.js",
                                "/manutencao.css",
                                "/js/**",
                                "/css/**",
                                "/img/**",
                                "/foto_ocorrencia/**",
                                "/foto_pessoa/**",
                                "/foto_flange/**"
                        ).permitAll()

                        // Telas de consulta disponíveis para os dois perfis.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/flange/listagem",
                                "/funcionario/listagem",
                                "/manutencao/listagem",
                                "/manutencao/cadastro",
                                "/perfil"
                        ).hasAnyRole("ADMIN", "USER")

                        // USER pode apenas criar novo lançamento de manutenção.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/manutencao/salvar"
                        ).hasAnyRole("ADMIN", "USER")

                        // USER pode abrir o formulário apenas para editar registros próprios;
                        // o ManutencaoController valida autoria e unidade antes de exibir/salvar.
                        .requestMatchers(HttpMethod.GET, "/manutencao/alterar/**")
                        .hasAnyRole("ADMIN", "USER")

                        // API de autocomplete já limita resultados à unidade do USER.
                        .requestMatchers("/api/flanges/**").hasAnyRole("ADMIN", "USER")

                        // Atalho de desenvolvimento: o próprio controller exige perfil dev e loopback.
                        .requestMatchers(HttpMethod.POST, "/experimental/admin-login").permitAll()

                        // Operações do próprio perfil autenticado.
                        .requestMatchers("/perfil/**").authenticated()

                        // Gestão de ativos, pessoas, unidades e correções fica com ADMIN.
                        .requestMatchers(
                                "/usuarios/**",
                                "/sensores/**",
                                "/analises",
                                "/analises/**",
                                "/usuario/**",
                                "/unidades/**",
                                "/flange/**",
                                "/funcionario/**",
                                "/manutencao/**",
                                "/incidente/**"
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
