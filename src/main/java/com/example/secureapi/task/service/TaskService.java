package com.example.secureapi.task.service;

import com.example.secureapi.common.PageResponse;
import com.example.secureapi.exception.ResourceNotFoundException;
import com.example.secureapi.task.dto.CreateTaskRequest;
import com.example.secureapi.task.dto.TaskResponse;
import com.example.secureapi.task.dto.UpdateTaskRequest;
import com.example.secureapi.task.entity.Task;
import com.example.secureapi.task.entity.TaskPriority;
import com.example.secureapi.task.entity.TaskStatus;
import com.example.secureapi.task.repository.TaskRepository;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Todas las operaciones de usuario reciben el id del usuario autenticado (obtenido del
 * token en el backend) y filtran por él. Nunca se confía en un id de propietario
 * enviado por el cliente.
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final String TASK_NOT_FOUND = "Task not found";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse create(Long ownerId, CreateTaskRequest request) {
        User owner = userRepository.getReferenceById(ownerId);
        TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;
        Task task = new Task(request.title().trim(), request.description(), TaskStatus.PENDING, priority, owner);
        Task saved = taskRepository.saveAndFlush(task);
        log.info("Task id={} created by user id={}", saved.getId(), ownerId);
        return TaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findAllForOwner(Long ownerId, Pageable pageable) {
        return PageResponse.from(taskRepository.findAllByUserId(ownerId, pageable), TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse findForOwner(Long taskId, Long ownerId) {
        return TaskResponse.from(findOwnedTask(taskId, ownerId));
    }

    @Transactional
    public TaskResponse update(Long taskId, Long ownerId, UpdateTaskRequest request) {
        Task task = findOwnedTask(taskId, ownerId);
        task.update(request.title().trim(), request.description(), request.status(), request.priority());
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void delete(Long taskId, Long ownerId) {
        Task task = findOwnedTask(taskId, ownerId);
        taskRepository.delete(task);
        log.info("Task id={} deleted by user id={}", taskId, ownerId);
    }

    // --- Administración ---

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findAll(Pageable pageable) {
        return PageResponse.from(taskRepository.findAll(pageable), TaskResponse::from);
    }

    /**
     * Una tarea ajena se trata igual que una inexistente (404) para no revelar
     * qué ids existen y pertenecen a otros usuarios.
     */
    private Task findOwnedTask(Long taskId, Long ownerId) {
        return taskRepository.findByIdAndUserId(taskId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND));
    }
}
