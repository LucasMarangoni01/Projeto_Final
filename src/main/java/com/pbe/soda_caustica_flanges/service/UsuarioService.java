package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAuditoriaService auditoriaService;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            UsuarioAuditoriaService auditoriaService) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    public boolean emailExiste(String email) {
        return repository.findByEmail(normalizarEmail(email)).isPresent();
    }

    @Transactional
    public Usuario cadastrar(CadastroDTO cadastroDTO) {
        Usuario usuario = new Usuario();
        usuario.setNome(cadastroDTO.getNome().trim());
        usuario.setEmail(normalizarEmail(cadastroDTO.getEmail()));
        usuario.setSenha(passwordEncoder.encode(cadastroDTO.getSenha()));
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);

        Usuario salvo = repository.save(usuario);
        auditoriaService.registrar(
                salvo,
                salvo.getEmail(),
                "CONTA_CRIADA",
                "Conta criada pelo formulário de cadastro."
        );
        return salvo;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuário não encontrado.")
                );
    }

    public Usuario buscarPorEmail(String email) {
        return repository.findByEmail(normalizarEmail(email))
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuário não encontrado.")
                );
    }

    @Transactional
    public Usuario atualizarPerfil(
            String email,
            PerfilDTO dados,
            String novaFoto,
            boolean removerFoto) {

        Usuario usuario = buscarPorEmail(email);
        usuario.setNome(dados.getNome().trim());
        usuario.setTelefone(normalizarCampo(dados.getTelefone()));
        usuario.setCargo(normalizarCampo(dados.getCargo()));
        usuario.setSetor(normalizarCampo(dados.getSetor()));
        usuario.setMatricula(normalizarCampo(dados.getMatricula()));

        if (novaFoto != null && !novaFoto.isBlank()) {
            usuario.setFotoPerfil(novaFoto);
        } else if (removerFoto) {
            usuario.setFotoPerfil(null);
        }

        Usuario salvo = repository.save(usuario);
        auditoriaService.registrar(
                salvo,
                salvo.getEmail(),
                "PERFIL_ATUALIZADO",
                "Dados de contato e identificação profissional atualizados."
        );

        if (novaFoto != null && !novaFoto.isBlank()) {
            auditoriaService.registrar(
                    salvo,
                    salvo.getEmail(),
                    "FOTO_PERFIL_ATUALIZADA",
                    "Foto de perfil alterada."
            );
        } else if (removerFoto) {
            auditoriaService.registrar(
                    salvo,
                    salvo.getEmail(),
                    "FOTO_PERFIL_REMOVIDA",
                    "Foto de perfil removida."
            );
        }

        return salvo;
    }

    @Transactional
    public void alterarSenha(String email, AlterarSenhaDTO dados) {
        Usuario usuario = buscarPorEmail(email);

        if (!passwordEncoder.matches(dados.getSenhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }

        if (!dados.getNovaSenha().equals(dados.getConfirmarSenha())) {
            throw new IllegalArgumentException("A confirmação da nova senha não confere.");
        }

        if (passwordEncoder.matches(dados.getNovaSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        if (dados.getNovaSenha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A nova senha ultrapassa o limite seguro de 72 bytes.");
        }

        usuario.setSenha(passwordEncoder.encode(dados.getNovaSenha()));
        Usuario salvo = repository.save(usuario);

        auditoriaService.registrar(
                salvo,
                salvo.getEmail(),
                "SENHA_ALTERADA",
                "Senha alterada pelo próprio titular da conta."
        );
    }

    @Transactional
    public void alterarRole(Long id, Role role, String emailLogado) {
        Usuario usuario = buscarPorId(id);

        if (usuario.getEmail().equalsIgnoreCase(emailLogado)
                && usuario.getRole() == Role.ADMIN
                && role != Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Você não pode remover sua própria permissão de ADMIN."
            );
        }

        if (usuario.isAtivo()
                && usuario.getRole() == Role.ADMIN
                && role != Role.ADMIN
                && repository.countByRoleAndAtivoTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException(
                    "Não é possível remover a última conta ADMIN ativa."
            );
        }

        Role roleAnterior = usuario.getRole();
        usuario.setRole(role);
        Usuario salvo = repository.save(usuario);

        auditoriaService.registrar(
                salvo,
                emailLogado,
                "PERMISSAO_ALTERADA",
                "Permissão alterada de " + roleAnterior + " para " + role + "."
        );
    }

    @Transactional
    public void alterarStatus(Long id, String emailLogado) {
        Usuario usuario = buscarPorId(id);

        if (usuario.getEmail().equalsIgnoreCase(emailLogado)) {
            throw new IllegalArgumentException(
                    "Você não pode desativar a própria conta."
            );
        }

        if (usuario.isAtivo()
                && usuario.getRole() == Role.ADMIN
                && repository.countByRoleAndAtivoTrue(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException(
                    "Não é possível desativar a última conta ADMIN ativa."
            );
        }

        usuario.setAtivo(!usuario.isAtivo());
        Usuario salvo = repository.save(usuario);

        auditoriaService.registrar(
                salvo,
                emailLogado,
                salvo.isAtivo() ? "CONTA_ATIVADA" : "CONTA_DESATIVADA",
                salvo.isAtivo()
                        ? "Conta ativada por um administrador."
                        : "Conta desativada por um administrador."
        );
    }

    @Transactional
    public void alterarUnidade(Long id, Unidade unidade, String emailLogado) {
        Usuario usuario = buscarPorId(id);
        String unidadeAnterior = usuario.getUnidade() == null
                ? "SEM UNIDADE"
                : usuario.getUnidade().getNome();

        usuario.setUnidade(unidade);
        Usuario salvo = repository.save(usuario);

        auditoriaService.registrar(
                salvo,
                emailLogado,
                "UNIDADE_ALTERADA",
                "Unidade alterada de " + unidadeAnterior + " para " + unidade.getNome() + "."
        );
    }

    private String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarCampo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
