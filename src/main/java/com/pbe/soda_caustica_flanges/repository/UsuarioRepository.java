package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByUnidadeId(Long unidadeId);
}