package com.pbe.soda_caustica_flanges.service;

import com.pbe.soda_caustica_flanges.model.Usuario;
import com.pbe.soda_caustica_flanges.model.UsuarioAuditoria;
import com.pbe.soda_caustica_flanges.repository.UsuarioAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioAuditoriaService {

    private final UsuarioAuditoriaRepository repository;

    public UsuarioAuditoriaService(UsuarioAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(
            Usuario usuario,
            String atorEmail,
            String acao,
            String detalhes) {

        if (usuario == null || usuario.getId() == null) {
            return;
        }

        UsuarioAuditoria evento = new UsuarioAuditoria();
        evento.setUsuarioId(usuario.getId());
        evento.setUsuarioEmail(usuario.getEmail());
        evento.setAtorEmail(
                atorEmail == null || atorEmail.isBlank()
                        ? "SISTEMA"
                        : atorEmail.trim().toLowerCase()
        );
        evento.setAcao(acao);
        evento.setDetalhes(detalhes);

        repository.save(evento);
    }

    public List<UsuarioAuditoria> buscarPorUsuario(Long usuarioId) {
        return repository.findTop30ByUsuarioIdOrderByDataEventoDesc(usuarioId);
    }
}
