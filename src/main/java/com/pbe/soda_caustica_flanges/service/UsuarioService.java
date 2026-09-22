package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.CadastroDTO;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==============================
    // VERIFICAR E-MAIL
    // ==============================
    public boolean emailExiste(String email) {

        return repository.findByEmail(email).isPresent();
    }

    // ==============================
    // CADASTRAR USUÁRIO
    // ==============================
    public Usuario cadastrar(CadastroDTO cadastroDTO) {

        Usuario usuario = new Usuario();

        usuario.setNome(
                cadastroDTO.getNome().trim()
        );

        usuario.setEmail(
                cadastroDTO.getEmail().trim().toLowerCase()
        );

        usuario.setSenha(
                passwordEncoder.encode(
                        cadastroDTO.getSenha()
                )
        );

        // Cadastro público sempre cria USER
        usuario.setRole(Role.USER);

        usuario.setAtivo(true);

        return repository.save(usuario);
    }
}