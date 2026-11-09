package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.Role;
import com.pbe.soda_caustica_flanges.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "unidade")
    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByUnidadeId(Long unidadeId);

    List<Usuario> findByUnidadeIdOrderByNomeAsc(Long unidadeId);

    List<Usuario> findByUnidadeIsNullOrderByNomeAsc();

    List<Usuario> findByUnidadeIsNullAndAtivoTrueOrderByNomeAsc();

    List<Usuario> findByUnidadeIsNullAndAtivoTrueAndRoleOrderByNomeAsc(Role role);

    List<Usuario> findByNomeIgnoreCase(String nome);

    long countByUnidadeId(Long unidadeId);

    long countByUnidadeIdAndAtivoTrue(Long unidadeId);

    long countByRoleAndAtivoTrue(Role role);
}
