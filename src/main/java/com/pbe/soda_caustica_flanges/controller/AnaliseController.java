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
 * Consolida dados reais de incidentes, manutenções, flanges e sensores.
 *
 * A entidade Incidente guarda ano, mês, quantidade e localização, mas ainda
 * não classifica a ocorrência como vazamento ou outro tipo de incidente.
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

        anosComDados.add(anoAtual);
        anosComDados.add(anoSelecionado);

        Map<Month, Long> incidentesPorMes = new EnumMap<>(Month.class);
        Map<Month, Long> manutencoesPorMes = new EnumMap<>(Month.class);
        Map<Month, Long> concluidasPorMes = new EnumMap<>(Month.class);
        Map<Month, Long> emAndamentoPorMes = new EnumMap<>(Month.class);

        for (Month mes : Month.values()) {
            incidentesPorMes.put(mes, 0L);
            manutencoesPorMes.put(mes, 0L);
            concluidasPorMes.put(mes, 0L);
            emAndamentoPorMes.put(mes, 0L);
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
                    : Math.max(0, incidente.getQuantidade());

            incidentesPorMes.merge(incidente.getMes(), quantidade, Long::sum);
            totalIncidentes += quantidade;

            String localizacao = incidente.getLocalizacao() == null
                    || incidente.getLocalizacao().isBlank()
                    ? "Localização não informada"
                    : incidente.getLocalizacao().trim();

            incidentesPorLocalizacao.merge(localizacao, quantidade, Long::sum);
        }

        Map<StatusAtividadeManutencao, Long> manutencoesPorStatus =
                new EnumMap<>(StatusAtividadeManutencao.class);
        for (StatusAtividadeManutencao status : StatusAtividadeManutencao.values()) {
            manutencoesPorStatus.put(status, 0L);
        }

        Map<NivelRiscoManutencao, Long> manutencoesPorRisco =
                new EnumMap<>(NivelRiscoManutencao.class);
        for (NivelRiscoManutencao risco : NivelRiscoManutencao.values()) {
            manutencoesPorRisco.put(risco, 0L);
        }

        long totalManutencoes = 0;
        long manutencoesConcluidas = 0;
        long manutencoesEmAndamento = 0;
        long statusNaoInformado = 0;
        long riscoNaoClassificado = 0;

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
            StatusAtividadeManutencao status = manutencao.getStatusAtividade();
            NivelRiscoManutencao risco = manutencao.getNivelRisco();

            manutencoesPorMes.merge(mes, 1L, Long::sum);
            totalManutencoes++;

            if (manutencao.isStatusAtividadeInformado()) {
                manutencoesPorStatus.merge(status, 1L, Long::sum);
            } else {
                statusNaoInformado++;
            }

            if (manutencao.isNivelRiscoInformado()) {
                manutencoesPorRisco.merge(risco, 1L, Long::sum);
            } else {
                riscoNaoClassificado++;
            }

            if (status == StatusAtividadeManutencao.CONCLUIDA) {
                concluidasPorMes.merge(mes, 1L, Long::sum);
                manutencoesConcluidas++;
            }

            if (status == StatusAtividadeManutencao.EM_ANDAMENTO) {
                emAndamentoPorMes.merge(mes, 1L, Long::sum);
                manutencoesEmAndamento++;
            }
        }

        long maiorValorMensal = 0;
        List<AnaliseMensalDTO> dadosMensais = new ArrayList<>();

        for (Month mes : Month.values()) {
            maiorValorMensal = Math.max(maiorValorMensal, incidentesPorMes.get(mes));
            maiorValorMensal = Math.max(maiorValorMensal, manutencoesPorMes.get(mes));
        }

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

        // Arrays com dados reais para o Chart.js. Os valores zero representam
        // ausência de registros naquele mês/categoria, não dados de demonstração.
        List<Long> serieIncidentes = new ArrayList<>();
        List<Long> serieManutencoes = new ArrayList<>();
        List<Long> serieConcluidas = new ArrayList<>();
        List<Long> serieEmAndamento = new ArrayList<>();

        for (Month mes : Month.values()) {
            serieIncidentes.add(incidentesPorMes.get(mes));
            serieManutencoes.add(manutencoesPorMes.get(mes));
            serieConcluidas.add(concluidasPorMes.get(mes));
            serieEmAndamento.add(emAndamentoPorMes.get(mes));
        }

        List<String> statusLabels = new ArrayList<>();
        List<Long> statusValores = new ArrayList<>();
        for (StatusAtividadeManutencao status : StatusAtividadeManutencao.values()) {
            statusLabels.add(status.getDescricao());
            statusValores.add(manutencoesPorStatus.get(status));
        }
        if (statusNaoInformado > 0) {
            statusLabels.add("Não informado");
            statusValores.add(statusNaoInformado);
        }

        List<String> riscoLabels = new ArrayList<>();
        List<Long> riscoValores = new ArrayList<>();
        for (NivelRiscoManutencao risco : NivelRiscoManutencao.values()) {
            riscoLabels.add(risco.getDescricao());
            riscoValores.add(manutencoesPorRisco.get(risco));
        }
        if (riscoNaoClassificado > 0) {
            riscoLabels.add("Não classificado");
            riscoValores.add(riscoNaoClassificado);
        }

        List<String> localizacaoLabels = locaisOrdenados.stream()
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();
        List<Long> localizacaoValores = locaisOrdenados.stream()
                .limit(8)
                .map(Map.Entry::getValue)
                .toList();

        List<String> radarLabels = List.of("Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
                "Jul", "Ago", "Set", "Out", "Nov", "Dez");

        List<String> flangeLabels = new ArrayList<>();
        List<Long> flangeValores = new ArrayList<>();
        for (StatusFlange status : StatusFlange.values()) {
            flangeLabels.add(descricaoStatusFlange(status));
            flangeValores.add(contagemFlanges.get(status));
        }

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
        model.addAttribute("manutencoesConcluidas", manutencoesConcluidas);
        model.addAttribute("manutencoesEmAndamento", manutencoesEmAndamento);
        model.addAttribute("totalFlanges", flanges.size());
        model.addAttribute("flangesEmAlerta", flangesEmAlerta);
        model.addAttribute("totalSensores", sensores.size());
        model.addAttribute("sensoresInstalados", sensoresInstalados);
        model.addAttribute("calibracoesVencidas", calibracoesVencidas);

        model.addAttribute("chartMeses", MESES);
        model.addAttribute("chartIncidentes", serieIncidentes);
        model.addAttribute("chartManutencoes", serieManutencoes);
        model.addAttribute("chartConcluidas", serieConcluidas);
        model.addAttribute("chartEmAndamento", serieEmAndamento);
        model.addAttribute("chartStatusLabels", statusLabels);
        model.addAttribute("chartStatusValores", statusValores);
        model.addAttribute("chartRiscoLabels", riscoLabels);
        model.addAttribute("chartRiscoValores", riscoValores);
        model.addAttribute("chartLocalizacaoLabels", localizacaoLabels);
        model.addAttribute("chartLocalizacaoValores", localizacaoValores);
        model.addAttribute("chartRadarLabels", radarLabels);
        model.addAttribute("chartFlangeLabels", flangeLabels);
        model.addAttribute("chartFlangeValores", flangeValores);

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
