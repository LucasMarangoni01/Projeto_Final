package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.DashboardFlangeDTO;
import com.pbe.soda_caustica_flanges.model.Flange;
import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.StatusFlange;
import com.pbe.soda_caustica_flanges.model.Sensor;
import com.pbe.soda_caustica_flanges.model.StatusSensor;
import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import com.pbe.soda_caustica_flanges.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int POSICOES_VISUAIS = 6;

    private final FlangeRepository flangeRepository;
    private final SensorRepository sensorRepository;
    private final UsuarioRepository usuarioRepository;

    public HomeController(
            FlangeRepository flangeRepository,
            SensorRepository sensorRepository,
            UsuarioRepository usuarioRepository) {
        this.flangeRepository = flangeRepository;
        this.sensorRepository = sensorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String home(Model model, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow();

        List<Flange> registros;

        if (usuario.getRole() == Role.ADMIN) {
            registros = flangeRepository.findAll();
        } else if (usuario.getUnidade() == null || !usuario.getUnidade().isAtivo()) {
            registros = new ArrayList<>();
        } else {
            registros = flangeRepository.findByUnidadeId(usuario.getUnidade().getId());
        }

        // Ordem estável: as seis primeiras flanges ocupam as seis posições do desenho.
        registros.sort(Comparator.comparing(
                Flange::getId,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        List<Long> idsFlanges = registros.stream()
                .map(Flange::getId)
                .toList();

        List<Sensor> sensoresDoPainel = idsFlanges.isEmpty()
                ? List.of()
                : sensorRepository.findByFlange_IdIn(idsFlanges);

        Map<Long, List<Sensor>> sensoresPorFlange = sensoresDoPainel.stream()
                .collect(Collectors.groupingBy(sensor -> sensor.getFlange().getId()));

        List<DashboardFlangeDTO> flanges = new ArrayList<>();

        for (int i = 0; i < registros.size(); i++) {
            Flange flange = registros.get(i);
            StatusFlange status = flange.getStatus();
            List<Sensor> sensoresDaFlange = sensoresPorFlange.getOrDefault(flange.getId(), List.of());

            int sensoresInstalados = (int) sensoresDaFlange.stream()
                    .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                    .count();

            String tiposSensores = sensoresDaFlange.stream()
                    .map(Sensor::getTipo)
                    .filter(tipo -> tipo != null)
                    .map(tipo -> tipo.getDescricao())
                    .distinct()
                    .collect(joining(", "));

            flanges.add(new DashboardFlangeDTO(
                    i < POSICOES_VISUAIS ? i + 1 : 0,
                    flange.getId(),
                    flange.getNome_flange(),
                    flange.getLocalizacao(),
                    flange.getTemp_ambiente(),
                    flange.getData_entrada() == null
                            ? "—"
                            : flange.getData_entrada().format(FORMATO_DATA),
                    status == null ? StatusFlange.NORMAL.name() : status.name(),
                    flange.getFoto(),
                    sensoresDaFlange.size(),
                    sensoresInstalados,
                    tiposSensores
            ));
        }

        List<DashboardFlangeDTO> flangesMapa = flanges.stream()
                .filter(flange -> flange.posicao() > 0)
                .toList();

        // Os indicadores abaixo vêm do status salvo no banco, não da simulação visual.
        long totalNormais = contarStatus(flanges, StatusFlange.NORMAL);
        long totalAtencao = contarStatus(flanges, StatusFlange.ATENCAO);
        long totalCriticas = contarStatus(flanges, StatusFlange.CRITICO);
        long totalManutencao = contarStatus(flanges, StatusFlange.MANUTENCAO);
        long totalSensoresInstalados = sensoresDoPainel.stream()
                .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                .count();
        long flangesSemSensor = flanges.stream()
                .filter(flange -> flange.sensoresInstalados() == 0)
                .count();

        model.addAttribute("flanges", flanges);
        model.addAttribute("flangesMapa", flangesMapa);
        model.addAttribute("totalFlanges", flanges.size());
        model.addAttribute("totalNormais", totalNormais);
        model.addAttribute("totalAtencao", totalAtencao);
        model.addAttribute("totalCriticas", totalCriticas);
        model.addAttribute("totalManutencao", totalManutencao);
        model.addAttribute("totalSensores", sensoresDoPainel.size());
        model.addAttribute("totalSensoresInstalados", totalSensoresInstalados);
        model.addAttribute("flangesSemSensor", flangesSemSensor);
        model.addAttribute("ehAdmin", usuario.getRole() == Role.ADMIN);
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
