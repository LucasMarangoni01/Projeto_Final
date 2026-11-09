package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.Unidade;
import com.pbe.soda_caustica_flanges.model.UnidadeAuditoria;
import com.pbe.soda_caustica_flanges.repository.UnidadeAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UnidadeAuditoriaService {

    private final UnidadeAuditoriaRepository repository;

    public UnidadeAuditoriaService(UnidadeAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(Unidade unidade, String atorEmail, String acao, String detalhes) {
        if (unidade == null || unidade.getId() == null) {
            return;
        }

        UnidadeAuditoria evento = new UnidadeAuditoria();
        evento.setUnidadeId(unidade.getId());
        evento.setUnidadeNome(unidade.getNome());
        evento.setAtorEmail(
                atorEmail == null || atorEmail.isBlank()
                        ? "SISTEMA"
                        : atorEmail.trim().toLowerCase(Locale.ROOT)
        );
        evento.setAcao(acao);
        evento.setDetalhes(detalhes);
        repository.save(evento);
    }

    public List<UnidadeAuditoria> buscarPorUnidade(Long unidadeId) {
        return repository.findTop30ByUnidadeIdOrderByDataEventoDesc(unidadeId);
    }
}
