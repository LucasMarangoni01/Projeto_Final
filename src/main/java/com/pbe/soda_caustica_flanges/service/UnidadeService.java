package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.repository.UnidadeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UnidadeService {

    private final UnidadeRepository repository;

    public UnidadeService(UnidadeRepository repository) {
        this.repository = repository;
    }

    public List<Unidade> listarTodas() {
        return repository.findAll();
    }

    public List<Unidade> listarAtivas() {
        return repository.findAll()
                .stream()
                .filter(Unidade::isAtivo)
                .toList();
    }

    public Unidade buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unidade não encontrada."
                        )
                );
    }

    public Unidade salvar(Unidade unidade) {
        unidade.setNome(unidade.getNome().trim());
        unidade.setLocalizacao(
                unidade.getLocalizacao().trim()
        );

        if (unidade.getId() == null) {
            unidade.setAtivo(true);
        }

        return repository.save(unidade);
    }

    public void alterarStatus(Long id) {
        Unidade unidade = buscarPorId(id);

        unidade.setAtivo(!unidade.isAtivo());

        repository.save(unidade);
    }
}