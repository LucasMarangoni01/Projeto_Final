package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.*;
import com.pbe.soda_caustica_flanges.repository.FlangeRepository;
import com.pbe.soda_caustica_flanges.repository.SensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SensorService {

    private final SensorRepository sensorRepository;
    private final FlangeRepository flangeRepository;

    public SensorService(
            SensorRepository sensorRepository,
            FlangeRepository flangeRepository) {
        this.sensorRepository = sensorRepository;
        this.flangeRepository = flangeRepository;
    }

    @Transactional(readOnly = true)
    public List<Sensor> listarTodos() {
        return sensorRepository.findAllByOrderByCodigoIdentificacaoAsc();
    }

    @Transactional(readOnly = true)
    public Sensor buscarPorId(Long id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sensor não encontrado."));
    }

    @Transactional
    public Sensor salvar(SensorDTO dados) {
        String codigo = dados.getCodigoIdentificacao().trim().toUpperCase(Locale.ROOT);

        boolean codigoDuplicado = dados.getId() == null
                ? sensorRepository.existsByCodigoIdentificacaoIgnoreCase(codigo)
                : sensorRepository.existsByCodigoIdentificacaoIgnoreCaseAndIdNot(
                        codigo,
                        dados.getId()
                );

        if (codigoDuplicado) {
            throw new IllegalArgumentException(
                    "Já existe um sensor cadastrado com este código de identificação."
            );
        }

        if (dados.getUltimaCalibracao() != null
                && dados.getProximaCalibracao() != null
                && dados.getProximaCalibracao().isBefore(dados.getUltimaCalibracao())) {
            throw new IllegalArgumentException(
                    "A próxima calibração não pode ser anterior à última calibração."
            );
        }

        if (dados.getStatus() == StatusSensor.INSTALADO
                && dados.getDataInstalacao() == null) {
            throw new IllegalArgumentException(
                    "Informe a data de instalação para marcar o sensor como instalado."
            );
        }

        Flange flange = flangeRepository.findById(dados.getFlangeId())
                .orElseThrow(() -> new IllegalArgumentException("A flange selecionada não existe."));

        Sensor sensor = dados.getId() == null
                ? new Sensor()
                : buscarPorId(dados.getId());

        boolean mantendoVinculoExistente = sensor.getFlange() != null
                && sensor.getFlange().getId().equals(flange.getId());

        if (flange.getUnidade() != null
                && !flange.getUnidade().isAtivo()
                && !mantendoVinculoExistente) {
            throw new IllegalArgumentException(
                    "Não é possível vincular sensores a uma flange de unidade inativa."
            );
        }

        sensor.setCodigoIdentificacao(codigo);
        sensor.setTipo(dados.getTipo());
        sensor.setFabricante(normalizar(dados.getFabricante()));
        sensor.setModelo(normalizar(dados.getModelo()));
        sensor.setNumeroSerie(normalizar(dados.getNumeroSerie()));
        sensor.setFlange(flange);
        sensor.setStatus(dados.getStatus() == null
                ? StatusSensor.CADASTRADO
                : dados.getStatus());
        sensor.setDataInstalacao(dados.getDataInstalacao());
        sensor.setUltimaCalibracao(dados.getUltimaCalibracao());
        sensor.setProximaCalibracao(dados.getProximaCalibracao());
        sensor.setObservacoes(normalizar(dados.getObservacoes()));

        return sensorRepository.save(sensor);
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
