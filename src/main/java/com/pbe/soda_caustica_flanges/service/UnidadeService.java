package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UnidadeService {

    private final UnidadeRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final FlangeRepository flangeRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UnidadeAuditoriaService auditoriaService;
    private final UsuarioAuditoriaService usuarioAuditoriaService;

    public UnidadeService(
            UnidadeRepository repository,
            UsuarioRepository usuarioRepository,
            FlangeRepository flangeRepository,
            ManutencaoRepository manutencaoRepository,
            FuncionarioRepository funcionarioRepository,
            UnidadeAuditoriaService auditoriaService,
            UsuarioAuditoriaService usuarioAuditoriaService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.flangeRepository = flangeRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioAuditoriaService = usuarioAuditoriaService;
    }

    public List<UnidadeResumoDTO> listarResumos() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(unidade -> new UnidadeResumoDTO(
                        unidade,
                        usuarioRepository.countByUnidadeId(unidade.getId()),
                        usuarioRepository.countByUnidadeIdAndAtivoTrue(unidade.getId()),
                        funcionarioRepository.countByUnidadeId(unidade.getId()),
                        flangeRepository.countByUnidadeId(unidade.getId()),
                        manutencaoRepository.countByFlange_Unidade_Id(unidade.getId())
                ))
                .toList();
    }

    public List<Unidade> listarTodas() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    public List<Unidade> listarAtivas() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .filter(Unidade::isAtivo)
                .toList();
    }

    public Unidade buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Unidade não encontrada.")
                );
    }

    public List<Usuario> listarUsuariosVinculados(Long unidadeId) {
        return usuarioRepository.findByUnidadeIdOrderByNomeAsc(unidadeId);
    }

    public List<Usuario> listarUsuariosDisponiveis() {
        return usuarioRepository.findByUnidadeIsNullAndAtivoTrueAndRoleOrderByNomeAsc(Role.USER);
    }

    public long contarFuncionarios(Long unidadeId) {
        return funcionarioRepository.countByUnidadeId(unidadeId);
    }

    public long contarFlanges(Long unidadeId) {
        return flangeRepository.countByUnidadeId(unidadeId);
    }

    public long contarManutencoes(Long unidadeId) {
        return manutencaoRepository.countByFlange_Unidade_Id(unidadeId);
    }

    public List<UnidadeAuditoria> buscarHistorico(Long unidadeId) {
        return auditoriaService.buscarPorUnidade(unidadeId);
    }

    @Transactional
    public Unidade salvar(Unidade dados, String atorEmail) {
        Unidade unidade;
        boolean novaUnidade = dados.getId() == null;

        if (novaUnidade) {
            unidade = new Unidade();
            unidade.setAtivo(true);
        } else {
            unidade = buscarPorId(dados.getId());
        }

        String codigo = normalizarCampo(dados.getCodigo());
        validarCodigo(codigo, unidade.getId());

        String nomeAnterior = unidade.getNome();
        String localizacaoAnterior = unidade.getLocalizacao();

        unidade.setNome(dados.getNome().trim());
        unidade.setLocalizacao(dados.getLocalizacao().trim());
        unidade.setCodigo(codigo);
        unidade.setResponsavel(normalizarCampo(dados.getResponsavel()));
        unidade.setTelefoneContato(normalizarCampo(dados.getTelefoneContato()));
        unidade.setEmailContato(normalizarEmail(dados.getEmailContato()));
        unidade.setDescricao(normalizarCampo(dados.getDescricao()));

        Unidade salva = repository.save(unidade);

        String detalhes;
        if (novaUnidade) {
            detalhes = "Unidade criada. Código: "
                    + valorOuTraco(salva.getCodigo())
                    + "; localização: " + salva.getLocalizacao() + ".";
        } else {
            detalhes = "Dados da unidade atualizados. Nome anterior: "
                    + valorOuTraco(nomeAnterior)
                    + "; novo nome: " + salva.getNome()
                    + "; localização anterior: " + valorOuTraco(localizacaoAnterior)
                    + "; nova localização: " + salva.getLocalizacao() + ".";
        }

        auditoriaService.registrar(
                salva,
                atorEmail,
                novaUnidade ? "UNIDADE_CRIADA" : "DADOS_ATUALIZADOS",
                detalhes
        );

        return salva;
    }

    @Transactional
    public void alterarStatus(Long id, String atorEmail) {
        Unidade unidade = buscarPorId(id);
        unidade.setAtivo(!unidade.isAtivo());

        Unidade salva = repository.save(unidade);
        auditoriaService.registrar(
                salva,
                atorEmail,
                salva.isAtivo() ? "UNIDADE_REATIVADA" : "UNIDADE_DESATIVADA",
                salva.isAtivo()
                        ? "Unidade reativada. Os vínculos existentes foram preservados."
                        : "Unidade desativada/arquivada. Usuários, flanges e manutenções foram preservados."
        );
    }

    @Transactional
    public void excluirUnidade(Long id, String atorEmail) {
        Unidade unidade = buscarPorId(id);

        long usuarios = usuarioRepository.countByUnidadeId(id);
        long funcionarios = funcionarioRepository.countByUnidadeId(id);
        long flanges = flangeRepository.countByUnidadeId(id);

        if (usuarios > 0 || funcionarios > 0 || flanges > 0) {
            throw new IllegalArgumentException(
                    "Esta unidade possui " + usuarios + " conta(s), "
                            + funcionarios + " funcionário(s) e "
                            + flanges + " flange(s) vinculados. Arquive a unidade para preservar o histórico."
            );
        }

        auditoriaService.registrar(
                unidade,
                atorEmail,
                "UNIDADE_EXCLUIDA",
                "Unidade sem usuários, funcionários ou flanges vinculados removida definitivamente."
        );

        repository.delete(unidade);
    }

    @Transactional
    public Usuario adicionarUsuario(Long unidadeId, String identificador, String atorEmail) {
        Unidade unidade = buscarPorId(unidadeId);

        if (!unidade.isAtivo()) {
            throw new IllegalArgumentException(
                    "Ative a unidade antes de adicionar pessoas."
            );
        }

        Usuario usuario = resolverUsuario(identificador);

        if (usuario.getRole() != Role.USER) {
            throw new IllegalArgumentException(
                    "Somente contas USER podem ser vinculadas a uma unidade operacional. Contas ADMIN têm acesso global."
            );
        }

        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException(
                    "Ative a conta da pessoa antes de vinculá-la a uma unidade."
            );
        }

        if (usuario.getUnidade() != null) {
            if (usuario.getUnidade().getId().equals(unidadeId)) {
                throw new IllegalArgumentException(
                        "Essa pessoa já está vinculada a esta unidade."
                );
            }

            throw new IllegalArgumentException(
                    "Essa pessoa já pertence à unidade "
                            + usuario.getUnidade().getNome()
                            + ". Remova o vínculo anterior antes de transferi-la."
            );
        }

        usuario.setUnidade(unidade);
        Usuario salvo = usuarioRepository.save(usuario);

        usuarioAuditoriaService.registrar(
                salvo,
                atorEmail,
                "UNIDADE_VINCULADA",
                "Usuário vinculado à unidade " + unidade.getNome()
                        + " (UNIDADE #" + unidade.getId() + ")."
        );
        auditoriaService.registrar(
                unidade,
                atorEmail,
                "PESSOA_ADICIONADA",
                "Usuário " + salvo.getNome() + " (ID #" + salvo.getId() + ") vinculado à unidade."
        );

        return salvo;
    }

    @Transactional
    public void removerUsuario(Long unidadeId, Long usuarioId, String atorEmail) {
        Unidade unidade = buscarPorId(unidadeId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Pessoa não encontrada.")
                );

        if (usuario.getUnidade() == null
                || !usuario.getUnidade().getId().equals(unidadeId)) {
            throw new IllegalArgumentException(
                    "Essa pessoa não está vinculada a esta unidade."
            );
        }

        usuario.setUnidade(null);
        Usuario salvo = usuarioRepository.save(usuario);

        usuarioAuditoriaService.registrar(
                salvo,
                atorEmail,
                "UNIDADE_DESVINCULADA",
                "Usuário desvinculado da unidade " + unidade.getNome()
                        + " (UNIDADE #" + unidade.getId() + ")."
        );
        auditoriaService.registrar(
                unidade,
                atorEmail,
                "PESSOA_REMOVIDA",
                "Usuário " + salvo.getNome() + " (ID #" + salvo.getId() + ") desvinculado da unidade."
        );
    }

    public List<Funcionario> listarFuncionariosVinculados(Long unidadeId) {
        return funcionarioRepository.findByUnidadeIdOrderByIdAsc(unidadeId);
    }

    public List<Funcionario> listarFuncionariosDisponiveis() {
        return funcionarioRepository.findByUnidadeIsNullOrderByIdAsc();
    }

    @Transactional
    public Funcionario adicionarFuncionario(
            Long unidadeId,
            String identificador,
            String atorEmail) {

        Unidade unidade = buscarPorId(unidadeId);
        if (!unidade.isAtivo()) {
            throw new IllegalArgumentException(
                    "Ative a unidade antes de adicionar funcionários."
            );
        }

        Funcionario funcionario = resolverFuncionario(identificador);

        if (funcionario.getUnidade() != null) {
            if (funcionario.getUnidade().getId().equals(unidadeId)) {
                throw new IllegalArgumentException(
                        "Esse funcionário já está vinculado a esta unidade."
                );
            }

            throw new IllegalArgumentException(
                    "Esse funcionário já pertence à unidade "
                            + funcionario.getUnidade().getNome()
                            + ". Remova o vínculo anterior antes de transferi-lo."
            );
        }

        funcionario.setUnidade(unidade);
        Funcionario salvo = funcionarioRepository.save(funcionario);

        auditoriaService.registrar(
                unidade,
                atorEmail,
                "FUNCIONARIO_ADICIONADO",
                "Funcionário " + salvo.getNome_funcionario()
                        + " (ID #" + salvo.getId() + ") vinculado à unidade."
        );

        return salvo;
    }

    @Transactional
    public void removerFuncionario(Long unidadeId, Long funcionarioId, String atorEmail) {
        Unidade unidade = buscarPorId(unidadeId);
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Funcionário não encontrado.")
                );

        if (funcionario.getUnidade() == null
                || !funcionario.getUnidade().getId().equals(unidadeId)) {
            throw new IllegalArgumentException(
                    "Esse funcionário não está vinculado a esta unidade."
            );
        }

        funcionario.setUnidade(null);
        Funcionario salvo = funcionarioRepository.save(funcionario);

        auditoriaService.registrar(
                unidade,
                atorEmail,
                "FUNCIONARIO_DESVINCULADO",
                "Funcionário " + salvo.getNome_funcionario()
                        + " (ID #" + salvo.getId() + ") desvinculado da unidade. "
                        + "O cadastro e os registros históricos foram preservados."
        );
    }

    @Transactional(readOnly = true)
    public List<Flange> listarFlangesVinculadas(Long unidadeId) {
        return flangeRepository.findByUnidadeIdOrderByIdAsc(unidadeId);
    }

    @Transactional(readOnly = true)
    public List<Flange> listarFlangesDisponiveis(Long unidadeId) {
        return flangeRepository.findForaDaUnidade(unidadeId);
    }

    @Transactional
    public Flange adicionarFlange(Long unidadeId, String identificador, String atorEmail) {
        Unidade unidade = buscarPorId(unidadeId);

        if (!unidade.isAtivo()) {
            throw new IllegalArgumentException("Ative a unidade antes de vincular flanges.");
        }

        Flange flange = resolverFlange(identificador);

        Unidade unidadeAnterior = flange.getUnidade();

        if (unidadeAnterior != null && unidadeAnterior.getId().equals(unidadeId)) {
            throw new IllegalArgumentException("Essa flange já está vinculada a esta unidade.");
        }

        flange.setUnidade(unidade);
        Flange salva = flangeRepository.save(flange);

        String detalhe = "Flange " + salva.getNome_flange() + " (ID #" + salva.getId()
                + ") vinculada à unidade " + unidade.getNome() + " (UNIDADE #" + unidadeId + ").";

        if (unidadeAnterior != null) {
            auditoriaService.registrar(
                    unidadeAnterior,
                    atorEmail,
                    "FLANGE_TRANSFERIDA",
                    "Flange " + salva.getNome_flange() + " (ID #" + salva.getId()
                            + ") transferida desta unidade para " + unidade.getNome()
                            + " (UNIDADE #" + unidadeId + ")."
            );
            detalhe = "Flange " + salva.getNome_flange() + " (ID #" + salva.getId()
                    + ") recebida por transferência da unidade " + unidadeAnterior.getNome()
                    + " (UNIDADE #" + unidadeAnterior.getId() + ").";
        }

        auditoriaService.registrar(
                unidade,
                atorEmail,
                unidadeAnterior == null ? "FLANGE_ADICIONADA" : "FLANGE_RECEBIDA",
                detalhe
        );

        return salva;
    }

    private Flange resolverFlange(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("Informe o ID ou o nome exato da flange.");
        }

        String termo = identificador.trim();
        String idInformado = termo.startsWith("#") ? termo.substring(1).trim() : termo;

        if (idInformado.matches("\\d+")) {
            try {
                return flangeRepository.findById(Long.parseLong(idInformado))
                        .orElseThrow(() ->
                                new IllegalArgumentException("Não existe flange com o ID informado.")
                        );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("O ID informado é inválido.");
            }
        }

        List<Flange> encontrados = flangeRepository.findAll().stream()
                .filter(flange -> flange.getNome_flange() != null
                        && flange.getNome_flange().equalsIgnoreCase(termo))
                .toList();

        if (encontrados.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não encontramos uma flange com esse nome. Confira a grafia ou informe o ID."
            );
        }

        if (encontrados.size() > 1) {
            throw new IllegalArgumentException(
                    "Há mais de uma flange com esse nome. Informe o ID para identificar o cadastro correto."
            );
        }

        return encontrados.get(0);
    }

    private Funcionario resolverFuncionario(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe o ID ou o nome completo do funcionário."
            );
        }

        String termo = identificador.trim();
        String idInformado = termo.startsWith("#")
                ? termo.substring(1).trim()
                : termo;

        if (idInformado.matches("\\d+")) {
            try {
                return funcionarioRepository.findById(Long.parseLong(idInformado))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Não existe funcionário com o ID informado."
                                )
                        );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("O ID informado é inválido.");
            }
        }

        List<Funcionario> encontrados = funcionarioRepository.buscarPorNomeExato(termo);

        if (encontrados.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não encontramos um funcionário com esse nome. Confira a grafia ou informe o ID."
            );
        }

        if (encontrados.size() > 1) {
            throw new IllegalArgumentException(
                    "Há mais de um funcionário com esse nome. Informe o ID para identificar o cadastro correto."
            );
        }

        return encontrados.get(0);
    }

    private Usuario resolverUsuario(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("Informe o ID ou o nome completo da pessoa.");
        }

        String termo = identificador.trim();
        String idInformado = termo.startsWith("#") ? termo.substring(1).trim() : termo;

        if (idInformado.matches("\\d+")) {
            try {
                return usuarioRepository.findById(Long.parseLong(idInformado))
                        .orElseThrow(() ->
                                new IllegalArgumentException("Não existe usuário com o ID informado.")
                        );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("O ID informado é inválido.");
            }
        }

        List<Usuario> encontrados = usuarioRepository.findByNomeIgnoreCase(termo);

        if (encontrados.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não encontramos uma pessoa com esse nome. Confira a grafia ou informe o ID."
            );
        }

        if (encontrados.size() > 1) {
            throw new IllegalArgumentException(
                    "Há mais de uma pessoa com esse nome. Informe o ID para identificar a conta correta."
            );
        }

        return encontrados.get(0);
    }

    private void validarCodigo(String codigo, Long idUnidade) {
        if (codigo == null) {
            return;
        }

        boolean existe = idUnidade == null
                ? repository.existsByCodigoIgnoreCase(codigo)
                : repository.existsByCodigoIgnoreCaseAndIdNot(codigo, idUnidade);

        if (existe) {
            throw new IllegalArgumentException(
                    "Esse código interno já está sendo utilizado por outra unidade."
            );
        }
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarCampo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}
