package com.example.secureapi.task.controller;

import com.example.secureapi.common.PageRequests;
import com.example.secureapi.common.PageResponse;
import com.example.secureapi.task.dto.TaskResponse;
import com.example.secureapi.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Tasks")
public class AdminTaskController {

    private final TaskService taskService;

    public AdminTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Listar las tareas de todos los usuarios (paginado)")
    public PageResponse<TaskResponse> findAll(
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) @Min(1) @Max(PageRequests.MAX_SIZE) int size) {
        return taskService.findAll(PageRequests.newestFirst(page, size));
    }
}
