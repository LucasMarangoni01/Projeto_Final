package com.pbe.soda_caustica_flanges.controller;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Month;
import java.util.*;

/**
 * Consolida os dados históricos e operacionais já persistidos.
 *
 * Observação: a entidade Incidente guarda ano, mês, quantidade e localização.
 * Ela ainda não possui um campo que classifique o tipo da ocorrência.
 */
@Controller
@RequestMapping("/analises")
public class AnaliseController {

    private static final List<String> MESES = List.of(
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    );

    private static final Locale LOCALE_PT_BR = new Locale("pt", "BR");

    private final IncidenteRepository incidenteRepository;
    private final ManutencaoRepository manutencaoRepository;
    private final FlangeRepository flangeRepository;
    private final SensorRepository sensorRepository;

    public AnaliseController(
            IncidenteRepository incidenteRepository,
            ManutencaoRepository manutencaoRepository,
            FlangeRepository flangeRepository,
            SensorRepository sensorRepository) {
        this.incidenteRepository = incidenteRepository;
        this.manutencaoRepository = manutencaoRepository;
        this.flangeRepository = flangeRepository;
        this.sensorRepository = sensorRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(name = "ano", required = false) Integer anoSolicitado,
            Model model) {

        List<Incidente> todosIncidentes = incidenteRepository.findAll();
        List<Manutencao> todasManutencoes = manutencaoRepository.findAllByOrderByDataDesc();
        List<Flange> flanges = flangeRepository.findAll();
        List<Sensor> sensores = sensorRepository.findAll();

        TreeSet<Integer> anosComDados = new TreeSet<>();

        for (Incidente incidente : todosIncidentes) {
            if (incidente.getAno() != null) {
                anosComDados.add(incidente.getAno());
            }
        }

        for (Manutencao manutencao : todasManutencoes) {
            if (manutencao.getData() != null) {
                Calendar calendario = Calendar.getInstance();
                calendario.setTime(manutencao.getData());
                anosComDados.add(calendario.get(Calendar.YEAR));
            }
        }

        int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
        int anoSelecionado = anoSolicitado != null
                ? anoSolicitado
                : (anosComDados.isEmpty() ? anoAtual : anosComDados.last());

        // Mantém o ano selecionado disponível no filtro, mesmo quando ainda não há dados nele.
        anosComDados.add(anoAtual);
        anosComDados.add(anoSelecionado);

        Map<Month, Long> incidentesPorMes = new EnumMap<>(Month.class);
        Map<Month, Long> manutencoesPorMes = new EnumMap<>(Month.class);

        for (Month mes : Month.values()) {
            incidentesPorMes.put(mes, 0L);
            manutencoesPorMes.put(mes, 0L);
        }

        Map<String, Long> incidentesPorLocalizacao = new HashMap<>();
        long totalIncidentes = 0;

        for (Incidente incidente : todosIncidentes) {
            if (!Objects.equals(incidente.getAno(), anoSelecionado)
                    || incidente.getMes() == null) {
                continue;
            }

            long quantidade = incidente.getQuantidade() == null
                    ? 0L
                    : incidente.getQuantidade();

            incidentesPorMes.merge(incidente.getMes(), quantidade, Long::sum);
            totalIncidentes += quantidade;

            String localizacao = incidente.getLocalizacao() == null
                    || incidente.getLocalizacao().isBlank()
                    ? "Localização não informada"
                    : incidente.getLocalizacao().trim();

            incidentesPorLocalizacao.merge(localizacao, quantidade, Long::sum);
        }

        long totalManutencoes = 0;

        for (Manutencao manutencao : todasManutencoes) {
            if (manutencao.getData() == null) {
                continue;
            }

            Calendar calendario = Calendar.getInstance();
            calendario.setTime(manutencao.getData());

            if (calendario.get(Calendar.YEAR) != anoSelecionado) {
                continue;
            }

            Month mes = Month.of(calendario.get(Calendar.MONTH) + 1);
            manutencoesPorMes.merge(mes, 1L, Long::sum);
            totalManutencoes++;
        }

        long maiorValorMensal = 0;
        for (Month mes : Month.values()) {
            maiorValorMensal = Math.max(maiorValorMensal, incidentesPorMes.get(mes));
            maiorValorMensal = Math.max(maiorValorMensal, manutencoesPorMes.get(mes));
        }

        List<AnaliseMensalDTO> dadosMensais = new ArrayList<>();

        for (int indice = 0; indice < 12; indice++) {
            Month mes = Month.of(indice + 1);
            long incidentes = incidentesPorMes.get(mes);
            long manutencoes = manutencoesPorMes.get(mes);

            dadosMensais.add(new AnaliseMensalDTO(
                    MESES.get(indice),
                    incidentes,
                    manutencoes,
                    percentualEscala(incidentes, maiorValorMensal),
                    percentualEscala(manutencoes, maiorValorMensal)
            ));
        }

        List<Map.Entry<String, Long>> locaisOrdenados = new ArrayList<>(
                incidentesPorLocalizacao.entrySet()
        );
        locaisOrdenados.sort(
                Map.Entry.<String, Long>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
        );

        long maiorLocal = locaisOrdenados.isEmpty()
                ? 0L
                : locaisOrdenados.get(0).getValue();

        List<AnaliseBarraDTO> rankingLocalizacoes = locaisOrdenados.stream()
                .limit(8)
                .map(item -> new AnaliseBarraDTO(
                        item.getKey(),
                        item.getValue(),
                        percentualEscala(item.getValue(), maiorLocal)
                ))
                .toList();

        Map<StatusFlange, Long> contagemFlanges = new EnumMap<>(StatusFlange.class);
        for (StatusFlange status : StatusFlange.values()) {
            contagemFlanges.put(status, 0L);
        }

        for (Flange flange : flanges) {
            contagemFlanges.merge(flange.getStatus(), 1L, Long::sum);
        }

        List<AnaliseBarraDTO> distribuicaoFlanges = new ArrayList<>();
        for (StatusFlange status : StatusFlange.values()) {
            long quantidade = contagemFlanges.get(status);
            distribuicaoFlanges.add(new AnaliseBarraDTO(
                    descricaoStatusFlange(status),
                    quantidade,
                    percentualEscala(quantidade, flanges.size())
            ));
        }

        Map<StatusSensor, Long> contagemSensores = new EnumMap<>(StatusSensor.class);
        for (StatusSensor status : StatusSensor.values()) {
            contagemSensores.put(status, 0L);
        }

        long calibracoesVencidas = 0;
        long sensoresInstalados = 0;

        for (Sensor sensor : sensores) {
            StatusSensor status = sensor.getStatus();
            contagemSensores.merge(status, 1L, Long::sum);

            if (status == StatusSensor.INSTALADO) {
                sensoresInstalados++;
            }

            if (sensor.isCalibracaoVencida()) {
                calibracoesVencidas++;
            }
        }

        List<AnaliseBarraDTO> distribuicaoSensores = new ArrayList<>();
        for (StatusSensor status : StatusSensor.values()) {
            long quantidade = contagemSensores.get(status);
            distribuicaoSensores.add(new AnaliseBarraDTO(
                    status.getDescricao(),
                    quantidade,
                    percentualEscala(quantidade, sensores.size())
            ));
        }

        String mesPico = "Sem registros";
        long quantidadePico = 0;

        for (int indice = 0; indice < 12; indice++) {
            long quantidade = incidentesPorMes.get(Month.of(indice + 1));
            if (quantidade > quantidadePico) {
                quantidadePico = quantidade;
                mesPico = MESES.get(indice);
            }
        }

        long flangesEmAlerta = contagemFlanges.get(StatusFlange.ATENCAO)
                + contagemFlanges.get(StatusFlange.CRITICO);

        model.addAttribute("anoSelecionado", anoSelecionado);
        model.addAttribute("anosDisponiveis", anosComDados.descendingSet());
        model.addAttribute("dadosMensais", dadosMensais);
        model.addAttribute("rankingLocalizacoes", rankingLocalizacoes);
        model.addAttribute("distribuicaoFlanges", distribuicaoFlanges);
        model.addAttribute("distribuicaoSensores", distribuicaoSensores);

        model.addAttribute("totalIncidentes", totalIncidentes);
        model.addAttribute("mesPico", mesPico);
        model.addAttribute("quantidadePico", quantidadePico);
        model.addAttribute("mediaMensal", String.format(
                LOCALE_PT_BR, "%.1f", totalIncidentes / 12.0
        ));
        model.addAttribute("totalLocalizacoes", incidentesPorLocalizacao.size());
        model.addAttribute("totalManutencoes", totalManutencoes);
        model.addAttribute("totalFlanges", flanges.size());
        model.addAttribute("flangesEmAlerta", flangesEmAlerta);
        model.addAttribute("totalSensores", sensores.size());
        model.addAttribute("sensoresInstalados", sensoresInstalados);
        model.addAttribute("calibracoesVencidas", calibracoesVencidas);

        return "analises/index";
    }

    private int percentualEscala(long valor, long maiorValor) {
        if (valor <= 0 || maiorValor <= 0) {
            return 0;
        }

        return (int) Math.min(100, Math.round((valor * 100.0) / maiorValor));
    }

    private String descricaoStatusFlange(StatusFlange status) {
        return switch (status) {
            case NORMAL -> "Normal";
            case ATENCAO -> "Atenção";
            case CRITICO -> "Crítica";
            case MANUTENCAO -> "Em manutenção";
        };
    }
}
