package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import com.pbe.soda_caustica_flanges.service.RelatorioCsvService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Exportacao dos dados previstos no TAP (relatorios de ativos e historicos).
 * ADMIN tem visao global; USER so recebe os registros da unidade vinculada.
 * Nenhum registro e apagado ou alterado para gerar os arquivos.
 */
@Controller
@RequestMapping("/relatorios")
@Transactional(readOnly = true)
public class RelatorioController {

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final UsuarioRepository usuarios;
    private final FlangeRepository flanges;
    private final FuncionarioRepository funcionarios;
    private final ManutencaoRepository manutencoes;
    private final IncidenteRepository incidentes;
    private final LeituraSensorRepository leituras;
    private final RelatorioCsvService csv;

    public RelatorioController(
            UsuarioRepository usuarios,
            FlangeRepository flanges,
            FuncionarioRepository funcionarios,
            ManutencaoRepository manutencoes,
            IncidenteRepository incidentes,
            LeituraSensorRepository leituras,
            RelatorioCsvService csv) {
        this.usuarios = usuarios;
        this.flanges = flanges;
        this.funcionarios = funcionarios;
        this.manutencoes = manutencoes;
        this.incidentes = incidentes;
        this.leituras = leituras;
        this.csv = csv;
    }

    @GetMapping
    public String index(Authentication authentication, Model model) {
        Usuario usuario = usuario(authentication);
        model.addAttribute("escopoRelatorio",
                usuario.getRole() == Role.ADMIN
                        ? "Todas as unidades"
                        : usuario.getUnidade().getNome());
        return "relatorios/index";
    }

    @GetMapping("/exportar/{tipo}")
    public ResponseEntity<byte[]> exportar(@PathVariable String tipo, Authentication authentication) {
        Usuario usuario = usuario(authentication);
        byte[] conteudo = switch (tipo) {
            case "flanges" -> relatorioFlanges(usuario);
            case "funcionarios" -> relatorioFuncionarios(usuario);
            case "manutencoes" -> relatorioManutencoes(usuario);
            case "ocorrencias" -> relatorioOcorrencias(usuario);
            case "alertas" -> relatorioAlertas(usuario);
            case "leituras" -> relatorioLeituras(usuario);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Relatorio nao encontrado.");
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tipo + ".csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(conteudo);
    }

    private Usuario usuario(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Usuario usuario = usuarios.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!usuario.isAtivo() || (usuario.getRole() != Role.ADMIN
                && (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return usuario;
    }

    private boolean admin(Usuario usuario) {
        return usuario.getRole() == Role.ADMIN;
    }

    private List<Flange> selecionarFlanges(Usuario usuario) {
        return admin(usuario) ? flanges.findAll()
                : flanges.findByUnidadeIdOrderByIdAsc(usuario.getUnidade().getId());
    }

    private List<Funcionario> selecionarFuncionarios(Usuario usuario) {
        return admin(usuario) ? funcionarios.findAll()
                : funcionarios.findByUnidadeIdOrderByIdAsc(usuario.getUnidade().getId());
    }

    private List<Manutencao> selecionarManutencoes(Usuario usuario) {
        return admin(usuario) ? manutencoes.findAllByOrderByDataDesc()
                : manutencoes.findByFlange_Unidade_IdOrderByDataDesc(usuario.getUnidade().getId());
    }

    private List<Incidente> selecionarOcorrencias(Usuario usuario) {
        return admin(usuario) ? incidentes.findAll()
                : incidentes.findByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
                        usuario.getUnidade().getId());
    }

    private List<LeituraSensor> selecionarLeituras(Usuario usuario) {
        return admin(usuario) ? leituras.findAll()
                : leituras.findByFlange_Unidade_IdOrderByRecebidaEmDesc(usuario.getUnidade().getId());
    }

    private byte[] relatorioFlanges(Usuario usuario) {
        List<List<String>> registros = selecionarFlanges(usuario).stream().map(f -> List.of(
                texto(f.getId()), texto(f.getNomeFlange()), texto(f.getLocalizacao()),
                nomeUnidade(f.getUnidade()), texto(f.getStatus()), texto(f.isAtivo()),
                texto(f.getTempAmbiente()), texto(f.getDataEntrada()),
                texto(f.getNormaTecnica()), texto(f.getTipoFlange()),
                texto(f.getDiametroNominal()), texto(f.getClassePressao()),
                texto(f.getTipoFace()), texto(f.getMaterialFabricacao()),
                texto(f.getTipoJunta())
        )).toList();
        return csv.gerar(List.of("ID", "Flange", "Localizacao", "Unidade", "Status",
                "Ativa", "Temperatura ambiente (C)", "Data de entrada",
                "Norma tecnica", "Tipo", "Diametro nominal", "Classe de pressao",
                "Tipo de face", "Material", "Tipo de junta"), registros);
    }

    private byte[] relatorioFuncionarios(Usuario usuario) {
        List<List<String>> registros = selecionarFuncionarios(usuario).stream().map(f -> List.of(
                texto(f.getId()), texto(f.getNome_funcionario()),
                nomeUnidade(f.getUnidade()), texto(f.getFuncionario_flange())
        )).toList();
        return csv.gerar(List.of("ID", "Funcionario", "Unidade", "Referencia de flange"), registros);
    }

    private byte[] relatorioManutencoes(Usuario usuario) {
        List<List<String>> registros = selecionarManutencoes(usuario).stream().map(m -> List.of(
                texto(m.getId()), data(m.getData()), texto(m.getOcorrencia()),
                texto(m.getNum_serie()),
                m.getFlange() == null ? "" : texto(m.getFlange().getId()),
                m.getFlange() == null ? "" : texto(m.getFlange().getNomeFlange()),
                m.getFlange() == null ? "" : nomeUnidade(m.getFlange().getUnidade()),
                m.getFuncionario() == null ? "" : texto(m.getFuncionario().getNome_funcionario()),
                texto(m.getNivelRisco()), texto(m.getStatusAtividade()),
                texto(m.isArquivada()), dataHora(m.getRegistradoEm())
        )).toList();
        return csv.gerar(List.of("ID", "Data", "Ocorrencia", "Numero de serie",
                "ID flange", "Flange", "Unidade", "Funcionario responsavel",
                "Nivel de risco", "Status da atividade", "Arquivada", "Registrada em"), registros);
    }

    private byte[] relatorioOcorrencias(Usuario usuario) {
        List<List<String>> registros = selecionarOcorrencias(usuario).stream().map(i -> List.of(
                texto(i.getId()), dataHora(i.getDataHora()),
                texto(i.getAno()), texto(i.getMes()), texto(i.getQuantidade()),
                texto(i.getTipoOcorrencia()), texto(i.getLocalizacao()),
                i.getFlange() == null ? "" : texto(i.getFlange().getId()),
                i.getFlange() == null ? "" : texto(i.getFlange().getNomeFlange()),
                i.getFlange() == null ? "" : nomeUnidade(i.getFlange().getUnidade()),
                texto(i.getTemperatura()), texto(i.getVazamentoDetectado()),
                texto(i.getStatusAlarme())
        )).toList();
        return csv.gerar(List.of("ID", "Data e hora", "Ano", "Mes", "Quantidade",
                "Tipo de ocorrencia", "Localizacao", "ID flange", "Flange", "Unidade",
                "Temperatura (C)", "Sinal de possivel vazamento", "Status do alarme"), registros);
    }

    private byte[] relatorioAlertas(Usuario usuario) {
        List<List<String>> registros = selecionarOcorrencias(usuario).stream()
                .filter(i -> i.getDataHora() != null)
                .map(i -> List.of(
                        texto(i.getId()), dataHora(i.getDataHora()),
                        i.getFlange() == null ? "" : texto(i.getFlange().getNomeFlange()),
                        i.getFlange() == null ? "" : nomeUnidade(i.getFlange().getUnidade()),
                        texto(i.getStatusAlarme()), texto(i.getTipoOcorrencia()),
                        texto(i.getTemperatura()), texto(i.getVazamentoDetectado()),
                        dataHora(i.getReconhecidoEm()), texto(i.getReconhecidoPorEmail()),
                        texto(i.getJustificativaReconhecimento()),
                        dataHora(i.getNormalizadoEm()), texto(i.getNormalizadoPorEmail()),
                        texto(i.getJustificativaNormalizacao())
                )).toList();
        return csv.gerar(List.of("ID", "Disparado em", "Flange", "Unidade",
                "Status", "Tipo", "Temperatura (C)", "Sinal de possivel vazamento",
                "Reconhecido em", "Reconhecido por", "Justificativa reconhecimento",
                "Normalizado em", "Normalizado por", "Justificativa normalizacao"), registros);
    }

    private byte[] relatorioLeituras(Usuario usuario) {
        List<List<String>> registros = selecionarLeituras(usuario).stream().map(l -> List.of(
                texto(l.getId()), dataHora(l.getRecebidaEm()),
                l.getSensor() == null ? "" : texto(l.getSensor().getCodigoIdentificacao()),
                l.getFlange() == null ? "" : texto(l.getFlange().getNomeFlange()),
                l.getFlange() == null ? "" : nomeUnidade(l.getFlange().getUnidade()),
                texto(l.getTemperatura()), texto(l.getPh()),
                texto(l.getCondutividade()), texto(l.getPressaoDiferencial()),
                texto(l.getVazamentoDetectado()), texto(l.isAlarmeDisparado()),
                texto(l.getOrigem())
        )).toList();
        return csv.gerar(List.of("ID", "Recebida em", "Codigo do sensor",
                "Flange", "Unidade", "Temperatura (C)", "pH", "Condutividade",
                "Pressao diferencial", "Sinal de possivel vazamento", "Alarme disparado",
                "Origem da leitura"), registros);
    }

    private String nomeUnidade(Unidade unidade) {
        return unidade == null ? "" : texto(unidade.getNome());
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private String dataHora(LocalDateTime valor) {
        return valor == null ? "" : valor.format(DATA_HORA);
    }

    private String data(Date valor) {
        return valor == null ? "" : new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR")).format(valor);
    }
}
