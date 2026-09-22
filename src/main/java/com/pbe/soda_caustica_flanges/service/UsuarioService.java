package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.CadastroDTO;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExiste(String email) {
        return repository.findByEmail(email).isPresent();
    }

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

        usuario.setRole(Role.USER);
        usuario.setAtivo(true);

        return repository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuário não encontrado."
                        )
                );
    }

    public Usuario buscarPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuário não encontrado."
                        )
                );
    }

    public void alterarRole(
            Long id,
            Role role,
            String emailLogado) {

        Usuario usuario = buscarPorId(id);

        if (usuario.getEmail().equalsIgnoreCase(emailLogado)
                && usuario.getRole() == Role.ADMIN
                && role != Role.ADMIN) {

            throw new IllegalArgumentException(
                    "Você não pode remover sua própria permissão de ADMIN."
            );
        }

        usuario.setRole(role);

        repository.save(usuario);
    }

    public void alterarStatus(
            Long id,
            String emailLogado) {

        Usuario usuario = buscarPorId(id);

        if (usuario.getEmail().equalsIgnoreCase(emailLogado)) {
            throw new IllegalArgumentException(
                    "Você não pode desativar sua própria conta."
            );
        }

        usuario.setAtivo(!usuario.isAtivo());

        repository.save(usuario);
    }

    public void alterarUnidade(
            Long id,
            Unidade unidade) {

        Usuario usuario = buscarPorId(id);

        usuario.setUnidade(unidade);
        repository.save(usuario);
    }
}