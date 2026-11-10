package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import com.pbe.soda_caustica_flanges.repository.FuncionarioRepository;
import com.pbe.soda_caustica_flanges.repository.ManutencaoRepository;
import com.pbe.soda_caustica_flanges.repository.RegistroCadastroLockRepository;
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
    private final FuncionarioRepository funcionarioRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAuditoriaService auditoriaService;
    private final RegistroCadastroLockRepository registroCadastroLockRepository;

    public UsuarioService(
            UsuarioRepository repository,
            FuncionarioRepository funcionarioRepository,
            ManutencaoRepository manutencaoRepository,
            PasswordEncoder passwordEncoder,
            UsuarioAuditoriaService auditoriaService,
            RegistroCadastroLockRepository registroCadastroLockRepository) {

        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.registroCadastroLockRepository = registroCadastroLockRepository;
    }

    public boolean emailExiste(String email) {
        return repository.findByEmail(normalizarEmail(email)).isPresent();
    }

    @Transactional
    public Usuario cadastrar(CadastroDTO cadastroDTO) {
        // O bloqueio no banco impede que dois cadastros simultâneos recebam ADMIN.
        registroCadastroLockRepository.bloquearPorId(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "O controle do primeiro cadastro não foi inicializado. Reinicie a aplicação."
                ));

        boolean primeiroCadastro = repository.count() == 0;

        Usuario usuario = new Usuario();
        usuario.setNome(cadastroDTO.getNome().trim());
        usuario.setEmail(normalizarEmail(cadastroDTO.getEmail()));
        usuario.setSenha(passwordEncoder.encode(cadastroDTO.getSenha()));
        usuario.setRole(primeiroCadastro ? Role.ADMIN : Role.USER);        usuario.setAtivo(true);

        Usuario salvo = repository.save(usuario);
        sincronizarFuncionario(salvo);
        auditoriaService.registrar(
                salvo,
                salvo.getEmail(),
                "CONTA_CRIADA",
                primeiroCadastro
                        ? "Primeira conta criada no sistema e definida automaticamente como ADMIN."
                        : "Conta criada pelo formulário de cadastro com perfil USER."
        );
        return salvo;
    }

    @Transactional
    public Funcionario sincronizarFuncionario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException("Salve a conta antes de gerar o funcionário.");
        }

        Funcionario funcionario = funcionarioRepository.findByUsuarioId(usuario.getId())
                .orElse(null);
        boolean novoVinculo = funcionario == null;

        if (funcionario != null
                && !java.util.Objects.equals(
                        funcionario.getUnidade() == null ? null : funcionario.getUnidade().getId(),
                        usuario.getUnidade() == null ? null : usuario.getUnidade().getId()
                )
                && manutencaoRepository.countByFuncionario_Id(funcionario.getId()) > 0) {
            // Mantém a unidade histórica do funcionário referenciado por manutenções antigas.
            // A conta passa a ter um novo cadastro operacional para a unidade atual.
            Funcionario historico = funcionario;
            historico.setUsuario(null);
            funcionarioRepository.save(historico);

            funcionario = new Funcionario();
            funcionario.setNome_funcionario(usuario.getNome());
            funcionario.setData_nascimento(historico.getData_nascimento());
            funcionario.setIdade(historico.getIdade());
            funcionario.setFuncionario_flange(historico.getFuncionario_flange());
            novoVinculo = true;
        }

        if (funcionario == null) {
            List<Funcionario> candidatos = usuario.getUnidade() == null
                    ? funcionarioRepository.buscarSemContaPorNomeESemUnidade(usuario.getNome())
                    : funcionarioRepository.buscarSemContaPorNomeEUnidade(
                            usuario.getNome(),
                            usuario.getUnidade().getId()
                    );

            // Reaproveita somente um cadastro homônimo inequívoco para não duplicar
            // pessoas que já haviam sido cadastradas manualmente.
            if (candidatos.size() == 1) {
                funcionario = candidatos.get(0);
            } else if (usuario.getUnidade() != null) {
                List<Funcionario> semUnidade =
                        funcionarioRepository.buscarSemContaPorNomeESemUnidade(usuario.getNome());
                if (semUnidade.size() == 1 && candidatos.isEmpty()) {
                    funcionario = semUnidade.get(0);
                }
            }

            if (funcionario == null) {
                funcionario = new Funcionario();
                funcionario.setNome_funcionario(usuario.getNome());
            }
        }

        funcionario.setUsuario(usuario);
        funcionario.setNome_funcionario(usuario.getNome());
        funcionario.setUnidade(usuario.getUnidade());

        Funcionario salvo = funcionarioRepository.save(funcionario);

        if (novoVinculo) {
            auditoriaService.registrar(
                    usuario,
                    "SISTEMA",
                    "FUNCIONARIO_OPERACIONAL_VINCULADO",
                    "Cadastro operacional criado/sincronizado automaticamente com a conta. "
                            + "Funcionário ID #" + salvo.getId() + "."
            );
        }

        return salvo;
    }

    @Transactional
    public int sincronizarFuncionariosExistentes() {
        List<Usuario> usuarios = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        int total = 0;

        for (Usuario usuario : usuarios) {
            boolean jaVinculado = funcionarioRepository.findByUsuarioId(usuario.getId()).isPresent();
            sincronizarFuncionario(usuario);
            if (!jaVinculado) {
                total++;
            }
        }

        return total;
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

    public boolean podeVisualizarFoto(String nomeArquivo, String emailSolicitante) {
        Usuario solicitante = buscarPorEmail(emailSolicitante);
        if (solicitante.getRole() == Role.ADMIN) {
            return true;
        }
        return nomeArquivo != null && nomeArquivo.equals(solicitante.getFotoPerfil());
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
        usuario.setTelefone(formatarTelefone(dados.getTelefone()));

        if (novaFoto != null && !novaFoto.isBlank()) {
            usuario.setFotoPerfil(novaFoto);
        } else if (removerFoto) {
            usuario.setFotoPerfil(null);
        }

        Usuario salvo = repository.save(usuario);
        sincronizarFuncionario(salvo);
        auditoriaService.registrar(
                salvo,
                salvo.getEmail(),
                "PERFIL_ATUALIZADO",
                "Nome e telefone de contato atualizados pelo próprio usuário."
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
    public void alterarDadosProfissionais(
            Long id,
            DadosProfissionaisDTO dados,
            String emailAdministrador) {

        Usuario usuario = buscarPorId(id);

        String matriculaAnterior = normalizarCampo(usuario.getMatricula());
        String cargoAnterior = normalizarCampo(usuario.getCargo());
        String setorAnterior = normalizarCampo(usuario.getSetor());

        usuario.setMatricula(normalizarCampo(dados.getMatricula()));
        usuario.setCargo(normalizarCampo(dados.getCargo()));
        usuario.setSetor(normalizarCampo(dados.getSetor()));

        boolean houveAlteracao =
                !java.util.Objects.equals(matriculaAnterior, usuario.getMatricula())
                || !java.util.Objects.equals(cargoAnterior, usuario.getCargo())
                || !java.util.Objects.equals(setorAnterior, usuario.getSetor());

        if (!houveAlteracao) {
            return;
        }

        Usuario salvo = repository.save(usuario);
        auditoriaService.registrar(
                salvo,
                emailAdministrador,
                "DADOS_PROFISSIONAIS_ALTERADOS",
                "Matrícula, cargo ou setor atualizados por um administrador."
        );
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
        sincronizarFuncionario(salvo);

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
        sincronizarFuncionario(salvo);

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

    private String formatarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }

        String numeros = telefone.replaceAll("\\D", "");

        if (numeros.length() != 10 && numeros.length() != 11) {
            throw new IllegalArgumentException(
                    "Informe um telefone válido com DDD: (11) 99999-9999."
            );
        }

        String ddd = numeros.substring(0, 2);
        String numero = numeros.substring(2);
        int tamanhoPrefixo = numero.length() == 9 ? 5 : 4;

        return "(" + ddd + ") "
                + numero.substring(0, tamanhoPrefixo)
                + "-"
                + numero.substring(tamanhoPrefixo);
    }

    private String normalizarCampo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
