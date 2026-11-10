package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sensores")
public class SensorController {

    private final SensorService sensorService;
    private final FlangeRepository flangeRepository;

    public SensorController(
            SensorService sensorService,
            FlangeRepository flangeRepository) {
        this.sensorService = sensorService;
        this.flangeRepository = flangeRepository;
    }

    @GetMapping
    public String gestao(
            Model model,
            @RequestParam(required = false) String sucesso,
            @RequestParam(required = false) String erro) {
        prepararPagina(model, new SensorDTO(), sucesso, erro);
        return "sensor/gestao";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Sensor sensor = sensorService.buscarPorId(id);
        prepararPagina(model, new SensorDTO(sensor), null, null);
        return "sensor/gestao";
    }

    @PostMapping("/salvar")
    public String salvar(
            @Valid @ModelAttribute("sensorDTO") SensorDTO sensorDTO,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            prepararPagina(model, sensorDTO, null, null);
            return "sensor/gestao";
        }

        try {
            boolean edicao = sensorDTO.getId() != null;
            sensorService.salvar(sensorDTO);
            return "redirect:/sensores?sucesso=" + (edicao ? "atualizado" : "cadastrado");
        } catch (IllegalArgumentException e) {
            prepararPagina(model, sensorDTO, null, e.getMessage());
            return "sensor/gestao";
        }
    }

    private void prepararPagina(
            Model model,
            SensorDTO sensorDTO,
            String sucesso,
            String erro) {

        List<Sensor> sensores = sensorService.listarTodos();
        List<Flange> flanges = flangeRepository.findAll(
                Sort.by(Sort.Direction.ASC, "nome_flange")
        );

        Map<Long, List<Sensor>> porFlange = sensores.stream()
                .filter(sensor -> sensor.getFlange() != null)
                .collect(Collectors.groupingBy(
                        sensor -> sensor.getFlange().getId()
                ));

        List<SensorFlangeResumoDTO> cobertura = new ArrayList<>();

        for (Flange flange : flanges) {
            List<Sensor> vinculados = porFlange.getOrDefault(flange.getId(), List.of());
            List<TipoSensor> tipos = vinculados.stream()
                    .map(Sensor::getTipo)
                    .filter(tipo -> tipo != null)
                    .distinct()
                    .toList();

            long instalados = vinculados.stream()
                    .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                    .count();

            cobertura.add(new SensorFlangeResumoDTO(
                    flange.getId(),
                    flange.getNome_flange(),
                    flange.getUnidade() == null
                            ? "Sem unidade"
                            : flange.getUnidade().getNome(),
                    vinculados.size(),
                    instalados,
                    tipos
            ));
        }

        long sensoresInstalados = sensores.stream()
                .filter(sensor -> sensor.getStatus() == StatusSensor.INSTALADO)
                .count();

        long sensoresManutencao = sensores.stream()
                .filter(sensor -> sensor.getStatus() == StatusSensor.EM_MANUTENCAO)
                .count();

        long calibracoesVencidas = sensores.stream()
                .filter(Sensor::isCalibracaoVencida)
                .count();

        long flangesSemSensor = cobertura.stream()
                .filter(resumo -> resumo.getTotalSensores() == 0)
                .count();

        model.addAttribute("sensorDTO", sensorDTO);
        model.addAttribute("sensores", sensores);
        model.addAttribute("flanges", flanges);
        model.addAttribute("coberturaFlanges", cobertura);
        model.addAttribute("tiposSensor", TipoSensor.values());
        model.addAttribute("statusSensores", StatusSensor.values());
        model.addAttribute("totalSensores", sensores.size());
        model.addAttribute("sensoresInstalados", sensoresInstalados);
        model.addAttribute("sensoresManutencao", sensoresManutencao);
        model.addAttribute("calibracoesVencidas", calibracoesVencidas);
        model.addAttribute("flangesSemSensor", flangesSemSensor);
        model.addAttribute("dataHoje", LocalDate.now());
        model.addAttribute("sucesso", sucesso);
        model.addAttribute("erro", erro);
    }
}
