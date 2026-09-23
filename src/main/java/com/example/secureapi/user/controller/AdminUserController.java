package com.example.secureapi.user.controller;

import com.example.secureapi.common.PageRequests;
import com.example.secureapi.common.PageResponse;
import com.example.secureapi.security.UserPrincipal;
import com.example.secureapi.user.dto.UserResponse;
import com.example.secureapi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios (paginado)")
    public PageResponse<UserResponse> findAll(
            @RequestParam(defaultValue = PageRequests.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = PageRequests.DEFAULT_SIZE) @Min(1) @Max(PageRequests.MAX_SIZE) int size) {
        return userService.findAll(PageRequests.newestFirst(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por id")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Desbloquear un usuario")
    public UserResponse enable(@PathVariable Long id) {
        return userService.enable(id);
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Bloquear un usuario")
    public UserResponse disable(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal admin) {
        return userService.disable(id, admin.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un usuario y sus tareas")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal admin) {
        userService.delete(id, admin.id());
    }
}
