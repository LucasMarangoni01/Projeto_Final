package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.IncidenteRepository;
import com.pbe.soda_caustica_flanges.repository.LeituraSensorRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_LEITURA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int COLUNAS_MAPA = 4;
    private static final double[] POSICOES_X = {110, 325, 540, 755};

    private final FlangeRepository flangeRepository;
    private final SensorRepository sensorRepository;
    private final UsuarioRepository usuarioRepository;
    private final LeituraSensorRepository leituraSensorRepository;
    private final IncidenteRepository incidenteRepository;

    // Referência visual provisória: não significa periodicidade homologada.
    @Value("${app.monitoramento.prazo-leitura-minutos:15}")
    private long prazoLeituraMinutos = 15;

    public HomeController(
            FlangeRepository flangeRepository,
            SensorRepository sensorRepository,
            UsuarioRepository usuarioRepository,
            LeituraSensorRepository leituraSensorRepository,
            IncidenteRepository incidenteRepository) {
        this.flangeRepository = flangeRepository;
        this.sensorRepository = sensorRepository;
        this.usuarioRepository = usuarioRepository;
        this.leituraSensorRepository = leituraSensorRepository;
        this.incidenteRepository = incidenteRepository;
    }

    @GetMapping
    public String home(Model model, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow();

        boolean admin = usuario.getRole() == Role.ADMIN;
        List<Flange> registros;

        if (admin) {
            registros = flangeRepository.findAtivas();
        } else if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            registros = new ArrayList<>();
        } else {
            registros = flangeRepository.findAtivasPorUnidade(usuario.getUnidade().getId());
        }

        registros.sort(Comparator.comparing(
                Flange::getId,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        List<Long> idsFlanges = registros.stream()
                .map(Flange::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Sensor> sensoresDoPainel = idsFlanges.isEmpty()
                ? List.of()
                : sensorRepository.findByFlange_IdIn(idsFlanges);

        Map<Long, List<Sensor>> sensoresPorFlange = sensoresDoPainel.stream()
                .filter(sensor -> sensor.getFlange() != null)
                .collect(Collectors.groupingBy(sensor -> sensor.getFlange().getId()));

        // Usar a leitura do próprio sensor instalado, não uma leitura legada
        // de outro dispositivo que tenha sido vinculado à mesma flange.
        List<Long> idsInstalados = sensoresDoPainel.stream()
                .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                .map(Sensor::getId)
                .filter(Objects::nonNull)
                .toList();
        List<LeituraSensor> ultimasLeituras = idsInstalados.isEmpty()
                ? List.of()
                : leituraSensorRepository.findUltimasPorSensores(idsInstalados);

        List<LeituraSensor> leiturasRecentes = idsFlanges.isEmpty()
                ? List.of()
                : leituraSensorRepository.findTop10ByFlange_IdInOrderByRecebidaEmDesc(idsFlanges);

        Map<Long, LeituraSensor> ultimaLeituraPorSensor = new HashMap<>();
        Map<Long, LeituraSensor> ultimaLeituraPorFlange = new HashMap<>();
        for (LeituraSensor leitura : ultimasLeituras) {
            if (leitura.getSensor() == null || leitura.getSensor().getId() == null) continue;
            ultimaLeituraPorSensor.put(leitura.getSensor().getId(), leitura);
            if (leitura.getFlange() != null && leitura.getFlange().getId() != null) {
                ultimaLeituraPorFlange.merge(leitura.getFlange().getId(), leitura,
                        (a, b) -> a.getRecebidaEm().isAfter(b.getRecebidaEm()) ? a : b);
            }
        }
        LocalDateTime horarioConsulta = LocalDateTime.now();

        List<Incidente> alarmesAtivos = admin
                ? incidenteRepository.findByDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc()
                : usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()
                    ? List.of()
                    : incidenteRepository.findByFlange_Unidade_IdAndDataHoraIsNotNullAndNormalizadoEmIsNullOrderByDataHoraDesc(
                            usuario.getUnidade().getId()
                    );

        // Historico compacto para o painel: inclui alertas ativos e normalizados,
        // sem carregar toda a tabela nem expor dados de outras unidades.
        List<Incidente> historicoRecente = admin
                ? incidenteRepository.findTop6ByDataHoraIsNotNullOrderByDataHoraDesc()
                : usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()
                    ? List.of()
                    : incidenteRepository.findTop6ByFlange_Unidade_IdAndDataHoraIsNotNullOrderByDataHoraDesc(
                            usuario.getUnidade().getId()
                    );

        Map<Long, List<Incidente>> alarmesPorFlange = alarmesAtivos.stream()
                .filter(incidente -> incidente.getFlange() != null
                        && incidente.getFlange().getId() != null)
                .collect(Collectors.groupingBy(incidente -> incidente.getFlange().getId()));

        Set<Long> flangeComAlarme = alarmesAtivos.stream()
                .filter(incidente -> incidente.getFlange() != null)
                .map(incidente -> incidente.getFlange().getId())
                .collect(Collectors.toSet());

        int linhasMapa = Math.max(1, (int) Math.ceil(registros.size() / (double) COLUNAS_MAPA));
        int alturaMapa = 145 + (linhasMapa - 1) * 110;

        List<DashboardFlangeDTO> flanges = new ArrayList<>();

        for (int i = 0; i < registros.size(); i++) {
            Flange flange = registros.get(i);
            List<Sensor> sensoresDaFlange = sensoresPorFlange.getOrDefault(flange.getId(), List.of());

            int sensoresInstalados = (int) sensoresDaFlange.stream()
                    .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                    .count();

            String tiposSensores = sensoresDaFlange.stream()
                    .map(Sensor::getTipo)
                    .filter(Objects::nonNull)
                    .map(TipoSensor::getDescricao)
                    .distinct()
                    .collect(joining(", "));

            LeituraSensor ultimaLeitura = ultimaLeituraPorFlange.get(flange.getId());

            EstadoComunicacaoSensor comunicacao = EstadoComunicacaoSensor.avaliar(
                    sensoresDaFlange, ultimaLeituraPorSensor,
                    horarioConsulta, prazoLeituraMinutos);
            EstadoVisualFlangeDTO estadoVisual = EstadoVisualFlangeDTO.classificar(
                    flange,
                    alarmesPorFlange.getOrDefault(flange.getId(), List.of()),
                    comunicacao);


            int coluna = i % COLUNAS_MAPA;
            int linha = i / COLUNAS_MAPA;

            // Coordenadas informadas no cadastro prevalecem. Quando ausentes,
            // o sistema organiza todos os ativos automaticamente em uma grade.
            double mapaX = flange.getCoordenadaX() == null
                    ? POSICOES_X[coluna]
                    : 60 + (flange.getCoordenadaX() / 100.0) * 740;

            double mapaY = flange.getCoordenadaY() == null
                    ? 72 + linha * 110
                    : 55 + (flange.getCoordenadaY() / 100.0) * (alturaMapa - 110);

            flanges.add(new DashboardFlangeDTO(
                    i + 1,
                    flange.getId(),
                    flange.getNomeFlange(),
                    flange.getLocalizacao(),
                    flange.getTempAmbiente(),
                    flange.getDataEntrada() == null ? "—" : flange.getDataEntrada().format(FORMATO_DATA),
                    flange.getStatus().name(),
                    flange.getFoto(),
                    sensoresDaFlange.size(),
                    sensoresInstalados,
                    tiposSensores,
                    mapaX,
                    mapaY,
                    ultimaLeitura == null ? null : ultimaLeitura.getTemperatura(),
                    ultimaLeitura == null || ultimaLeitura.getRecebidaEm() == null
                            ? null
                            : ultimaLeitura.getRecebidaEm().format(FORMATO_LEITURA),
                    flangeComAlarme.contains(flange.getId()),
                    estadoVisual
            ));
        }

        long totalNormais = contarStatus(flanges, StatusFlange.NORMAL);
        long totalAtencao = contarStatus(flanges, StatusFlange.ATENCAO);
        long totalCriticas = contarStatus(flanges, StatusFlange.CRITICO);
        long totalManutencao = contarStatus(flanges, StatusFlange.MANUTENCAO);
        long totalSensoresInstalados = sensoresDoPainel.stream()
                .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                .count();
        long flangesSemSensor = flanges.stream()
                .filter(flange -> flange.totalSensores() == 0)
                .count();

        model.addAttribute("flanges", flanges);
        model.addAttribute("flangesMapa", flanges);
        model.addAttribute("alturaMapa", alturaMapa);
        model.addAttribute("linhasMapa", linhasMapa);
        model.addAttribute("totalFlanges", flanges.size());
        model.addAttribute("totalFlangesEmAlerta", flanges.stream()
                .filter(flange -> flange.estadoVisual().prioridade() >= 40)
                .count());
        model.addAttribute("totalNormais", totalNormais);
        model.addAttribute("totalAtencao", totalAtencao);
        model.addAttribute("totalCriticas", totalCriticas);
        model.addAttribute("totalManutencao", totalManutencao);
        model.addAttribute("totalSensores", sensoresDoPainel.size());
        model.addAttribute("totalSensoresInstalados", totalSensoresInstalados);
        model.addAttribute("flangesSemSensor", flangesSemSensor);
        model.addAttribute("totalAlertasAtivos", alarmesAtivos.size());
        model.addAttribute("leiturasRecentes", leiturasRecentes);
        model.addAttribute("eventosRecentes", historicoRecente.stream()
                .map(EventoMonitoramentoDTO::de)
                .toList());
        model.addAttribute("ehAdmin", admin);
        model.addAttribute("nomeUnidade", usuario.getUnidade() == null
                ? "Todas as unidades"
                : usuario.getUnidade().getNome());

        return "home/index";
    }

    private long contarStatus(List<DashboardFlangeDTO> flanges, StatusFlange status) {
        return flanges.stream()
                .filter(flange -> status.name().equals(flange.status()))
                .count();
    }
}
