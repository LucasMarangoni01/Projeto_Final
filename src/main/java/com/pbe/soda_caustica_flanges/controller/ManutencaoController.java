package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class ManutencaoController {

    private final ManutencaoRepository manutencaoRepository;
    private final FlangeRepository flangeRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioRepository usuarioRepository;

    public ManutencaoController(
            ManutencaoRepository manutencaoRepository,
            FlangeRepository flangeRepository,
            FuncionarioRepository funcionarioRepository,
            UsuarioRepository usuarioRepository) {
        this.manutencaoRepository = manutencaoRepository;
        this.flangeRepository = flangeRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/manutencao/listagem")
    public String listagem(
            Model model,
            Authentication authentication,
            @RequestParam(value = "erro", required = false) String erro) {

        Usuario usuario = buscarUsuarioLogado(authentication);
        boolean admin = usuario.getRole() == Role.ADMIN;

        List<Manutencao> manutencoes;
        if (admin) {
            manutencoes = manutencaoRepository.findAllByOrderByDataDesc();
        } else if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            manutencoes = Collections.emptyList();
        } else {
            manutencoes = manutencaoRepository.findByFlange_Unidade_IdOrderByDataDesc(
                    usuario.getUnidade().getId()
            );
        }

        model.addAttribute("manutencoes", manutencoes);
        model.addAttribute("ehAdmin", admin);
        model.addAttribute("erro", mensagemErroListagem(erro));
        return "manutencao/listagem";
    }

    @GetMapping("/manutencao/cadastro")
    public String cadastro(Model model, Authentication authentication) {
        Usuario usuario = buscarUsuarioLogado(authentication);
        Manutencao manutencao = new Manutencao();
        manutencao.setData(new java.util.Date());
        manutencao.setNivelRisco(NivelRiscoManutencao.MODERADO);
        manutencao.setStatusAtividade(StatusAtividadeManutencao.REGISTRADA);
        prepararFormulario(model, manutencao, usuario);
        return "manutencao/cadastro";
    }

    @PostMapping("/manutencao/salvar")
    public String salvar(
            @ModelAttribute("manutencao") Manutencao manutencao,
            @RequestParam(value = "flangeId", required = false) Long flangeId,
            @RequestParam(value = "funcionarioId", required = false) Long funcionarioId,
            Authentication authentication,
            Model model) {

        Usuario usuario = buscarUsuarioLogado(authentication);
        boolean admin = usuario.getRole() == Role.ADMIN;
        Manutencao registroExistente = null;

        // A autorização é feita no servidor: o USER só altera o próprio lançamento.
        if (manutencao.getId() != null) {
            registroExistente = manutencaoRepository.findById(manutencao.getId()).orElse(null);

            if (registroExistente == null) {
                return "redirect:/manutencao/listagem?erro=nao_encontrado";
            }

            if (!podeEditar(registroExistente, usuario, admin)) {
                return "redirect:/manutencao/listagem?erro=permissao";
            }

            // Mantém as associações atuais disponíveis caso a validação precise
            // devolver o formulário com uma mensagem de erro.
            manutencao.setFlange(registroExistente.getFlange());
            manutencao.setFuncionario(registroExistente.getFuncionario());
        }

        if (flangeId == null || funcionarioId == null) {
            List<Funcionario> funcionariosDisponiveis = listarFuncionariosPermitidos(usuario);

            String mensagem = funcionariosDisponiveis.isEmpty()
                    ? mensagemSemFuncionarios(usuario)
                    : "Selecione uma flange e um funcionário responsável.";

            model.addAttribute("erro", mensagem);
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (manutencao.getOcorrencia() == null || manutencao.getOcorrencia().isBlank()
                || manutencao.getNum_serie() == null || manutencao.getNum_serie().isBlank()
                || manutencao.getData() == null) {
            model.addAttribute("erro", "Preencha a descrição, a ordem/número de série e a data da manutenção.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        Flange flange = flangeRepository.findById(flangeId).orElse(null);
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId).orElse(null);

        if (flange == null || funcionario == null) {
            model.addAttribute("erro", "A flange ou o funcionário selecionado não existe.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (flange.getUnidade() == null || funcionario.getUnidade() == null
                || !flange.getUnidade().getId().equals(funcionario.getUnidade().getId())) {
            model.addAttribute("erro", "A flange e o funcionário precisam pertencer à mesma unidade.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (!admin) {
            if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()
                    || !flange.getUnidade().getId().equals(usuario.getUnidade().getId())) {
                model.addAttribute("erro", "Você só pode registrar ou editar manutenções em ativos da sua unidade.");
                prepararFormulario(model, manutencao, usuario);
                return "manutencao/cadastro";
            }
        } else if (!flange.getUnidade().isAtivo() && manutencao.getId() == null) {
            model.addAttribute("erro", "Não é possível registrar uma nova manutenção em uma unidade arquivada.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        StatusAtividadeManutencao statusSolicitado = manutencao.getStatusAtividade();

        if (!admin && statusSolicitado == StatusAtividadeManutencao.EM_ANDAMENTO
                && (registroExistente == null
                || registroExistente.getStatusAtividade() != StatusAtividadeManutencao.EM_ANDAMENTO)) {
            manutencao.setStatusAtividade(registroExistente == null
                    ? StatusAtividadeManutencao.REGISTRADA
                    : registroExistente.getStatusAtividade());
            model.addAttribute("erro", "Somente um ADMIN pode colocar uma atividade como Em andamento.");
            prepararFormulario(model, manutencao, usuario);
            return "manutencao/cadastro";
        }

        if (registroExistente == null) {
            // A autoria é definida pelo servidor e não pode ser enviada pelo formulário.
            manutencao.setRegistradoPorEmail(authentication.getName());
        } else {
            // Preserva a autoria e o horário original do lançamento.
            manutencao.setRegistradoPorEmail(registroExistente.getRegistradoPorEmail());
            manutencao.setRegistradoEm(registroExistente.getRegistradoEm());

            // Um USER que edita um item já em andamento não consegue alterar o status.
            if (!admin && registroExistente.getStatusAtividade()
                    == StatusAtividadeManutencao.EM_ANDAMENTO) {
                manutencao.setStatusAtividade(StatusAtividadeManutencao.EM_ANDAMENTO);
            }
        }

        if (manutencao.getNivelRisco() == null) {
            manutencao.setNivelRisco(NivelRiscoManutencao.MODERADO);
        }

        if (manutencao.getStatusAtividade() == null) {
            manutencao.setStatusAtividade(StatusAtividadeManutencao.REGISTRADA);
        }

        manutencao.setFlange(flange);
        manutencao.setFuncionario(funcionario);
        manutencaoRepository.save(manutencao);

        return "redirect:/manutencao/listagem";
    }

    @GetMapping("/manutencao/alterar/{id}")
    public String alterar(
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        Usuario usuario = buscarUsuarioLogado(authentication);
        boolean admin = usuario.getRole() == Role.ADMIN;
        Manutencao manutencao = manutencaoRepository.findById(id).orElse(null);

        if (manutencao == null) {
            return "redirect:/manutencao/listagem?erro=nao_encontrado";
        }

        if (!podeEditar(manutencao, usuario, admin)) {
            return "redirect:/manutencao/listagem?erro=permissao";
        }

        prepararFormulario(model, manutencao, usuario);
        return "manutencao/cadastro";
    }

    @PostMapping("/manutencao/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        manutencaoRepository.findById(id).ifPresent(manutencaoRepository::delete);
        return "redirect:/manutencao/listagem";
    }

    private boolean podeEditar(Manutencao manutencao, Usuario usuario, boolean admin) {
        if (admin) {
            return true;
        }

        if (manutencao.getRegistradoPorEmail() == null
                || usuario.getUnidade() == null
                || !usuario.getUnidade().isAtivo()
                || manutencao.getFlange() == null
                || manutencao.getFlange().getUnidade() == null) {
            return false;
        }

        boolean ehAutor = manutencao.getRegistradoPorEmail()
                .equalsIgnoreCase(usuario.getEmail());

        boolean mesmaUnidade = manutencao.getFlange().getUnidade().getId()
                .equals(usuario.getUnidade().getId());

        return ehAutor && mesmaUnidade;
    }

    private Usuario buscarUsuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado."));
    }

    private void prepararFormulario(Model model, Manutencao manutencao, Usuario usuario) {
        boolean admin = usuario.getRole() == Role.ADMIN;
        List<Flange> flanges;

        if (admin) {
            flanges = flangeRepository.findAll();
        } else if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            flanges = Collections.emptyList();
        } else {
            flanges = flangeRepository.findByUnidadeId(usuario.getUnidade().getId());
        }

        List<Funcionario> funcionarios = listarFuncionariosPermitidos(usuario);

        // Ao editar um registro histórico, preserva a pessoa que já estava associada,
        // mesmo que a unidade tenha sido arquivada posteriormente.
        if (manutencao.getFuncionario() != null
                && funcionarios.stream().noneMatch(f -> f.getId().equals(manutencao.getFuncionario().getId()))) {
            funcionarios.add(manutencao.getFuncionario());
        }

        funcionarios.sort(java.util.Comparator.comparing(
                Funcionario::getNome_funcionario,
                java.text.Collator.getInstance(new java.util.Locale("pt", "BR"))
        ));

        List<StatusAtividadeManutencao> statusDisponiveis =
                new ArrayList<>(List.of(StatusAtividadeManutencao.values()));

        boolean statusAtividadeBloqueado = !admin
                && manutencao.getStatusAtividade() == StatusAtividadeManutencao.EM_ANDAMENTO;

        if (!admin) {
            statusDisponiveis.remove(StatusAtividadeManutencao.EM_ANDAMENTO);
        }

        model.addAttribute("manutencao", manutencao);
        model.addAttribute("flanges", flanges);
        model.addAttribute("funcionarios", funcionarios);
        model.addAttribute("ehAdmin", admin);
        model.addAttribute("unidadeUsuario", usuario.getUnidade());
        model.addAttribute("nenhumFuncionarioDisponivel", funcionarios.isEmpty());
        model.addAttribute("niveisRisco", NivelRiscoManutencao.values());
        model.addAttribute("statusAtividades", statusDisponiveis);
        model.addAttribute("statusAtividadeBloqueado", statusAtividadeBloqueado);
    }

    private List<Funcionario> listarFuncionariosPermitidos(Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return new ArrayList<>(funcionarioRepository.findAll()
                    .stream()
                    .filter(funcionario -> funcionario.getUnidade() != null
                            && funcionario.getUnidade().isAtivo()
                            && (funcionario.getUsuario() == null || funcionario.getUsuario().isAtivo()))
                    .sorted(java.util.Comparator.comparing(
                            Funcionario::getNome_funcionario,
                            java.text.Collator.getInstance(new java.util.Locale("pt", "BR"))
                    ))
                    .toList());
        }

        if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                funcionarioRepository.findByUnidadeIdOrderByIdAsc(
                        usuario.getUnidade().getId()
                ).stream()
                        .filter(funcionario -> funcionario.getUsuario() == null
                                || funcionario.getUsuario().isAtivo())
                        .toList()
        );
    }

    private String mensagemSemFuncionarios(Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return "Não há funcionários operacionais cadastrados. Cadastre um funcionário antes de registrar a manutenção.";
        }

        String unidade = usuario.getUnidade() == null
                ? "sua unidade"
                : usuario.getUnidade().getNome();

        return "Não há funcionários operacionais vinculados a " + unidade
                + ". Solicite ao ADMIN que cadastre ou vincule um funcionário a essa unidade.";
    }

    private String mensagemErroListagem(String erro) {
        if ("permissao".equals(erro)) {
            return "Você só pode editar os registros de manutenção criados pela sua própria conta e pertencentes à sua unidade.";
        }
        if ("nao_encontrado".equals(erro)) {
            return "O registro de manutenção solicitado não foi encontrado.";
        }
        return null;
    }
}
