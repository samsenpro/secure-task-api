package com.example.secureapi.task.repository;

import com.example.secureapi.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findAllByUserId(Long userId, Pageable pageable);

    /**
     * Busca la tarea solo si pertenece al usuario indicado. Es la base del
     * control de acceso horizontal: una tarea ajena se comporta como inexistente.
     */
    Optional<Task> findByIdAndUserId(Long id, Long userId);
}
