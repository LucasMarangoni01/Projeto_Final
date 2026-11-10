package com.pbe.soda_caustica_flanges.repository;

import com.pbe.soda_caustica_flanges.model.RegistroCadastroLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RegistroCadastroLockRepository extends JpaRepository<RegistroCadastroLock, Long> {

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO registro_cadastro_lock (id) VALUES (1)", nativeQuery = true)
    void garantirRegistroInicial();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT registro FROM RegistroCadastroLock registro WHERE registro.id = :id")
    Optional<RegistroCadastroLock> bloquearPorId(@Param("id") Long id);
}
