package com.example.secureapi.task.controller;

import com.example.secureapi.common.PageRequests;
import com.example.secureapi.common.PageResponse;
import com.example.secureapi.security.UserPrincipal;
import com.example.secureapi.task.dto.CreateTaskRequest;
import com.example.secureapi.task.dto.TaskResponse;
import com.example.secureapi.task.dto.UpdateTaskRequest;
import com.example.secureapi.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Crear una tarea para el usuario autenticado")
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(principal.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar las tareas del usuario autenticado (paginado)")
    public PageResponse<TaskResponse> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) @Min(1) @Max(PageRequests.MAX_SIZE) int size) {
        return taskService.findAllForOwner(principal.id(), PageRequests.newestFirst(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una tarea propia")
    public TaskResponse findById(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return taskService.findForOwner(id, principal.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una tarea propia")
    public TaskResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, principal.id(), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una tarea propia")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        taskService.delete(id, principal.id());
        return ResponseEntity.noContent().build();
    }
}
