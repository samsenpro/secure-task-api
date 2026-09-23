package com.example.secureapi.task.service;

import com.example.secureapi.common.PageRequests;
import com.example.secureapi.common.PageResponse;
import com.example.secureapi.exception.ResourceNotFoundException;
import com.example.secureapi.task.dto.CreateTaskRequest;
import com.example.secureapi.task.dto.TaskResponse;
import com.example.secureapi.task.dto.UpdateTaskRequest;
import com.example.secureapi.task.entity.Task;
import com.example.secureapi.task.entity.TaskPriority;
import com.example.secureapi.task.entity.TaskStatus;
import com.example.secureapi.task.repository.TaskRepository;
import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.example.secureapi.support.TestFixtures.task;
import static com.example.secureapi.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private final User owner = user(OWNER_ID, "owner@example.com", Role.USER);

    @Test
    void createAssignsOwnerPendingStatusAndDefaultPriority() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            ReflectionTestUtils.setField(task, "id", 5L);
            return task;
        });

        TaskResponse response = taskService.create(OWNER_ID, new CreateTaskRequest(" Title ", "Desc", null));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(owner);
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.userId()).isEqualTo(OWNER_ID);
    }

    @Test
    void createKeepsRequestedPriority() {
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(OWNER_ID, new CreateTaskRequest("Title", null, TaskPriority.HIGH));

        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void findAllForOwnerOnlyQueriesTheOwnersTasks() {
        Pageable pageable = PageRequests.newestFirst(0, 20);
        when(taskRepository.findAllByUserId(OWNER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(task(1L, owner), task(2L, owner)), pageable, 2));

        PageResponse<TaskResponse> page = taskService.findAllForOwner(OWNER_ID, pageable);

        assertThat(page.content()).extracting(TaskResponse::id).containsExactly(1L, 2L);
        assertThat(page.totalElements()).isEqualTo(2);
        verify(taskRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findForOwnerReturnsOwnTask() {
        when(taskRepository.findByIdAndUserId(7L, OWNER_ID)).thenReturn(Optional.of(task(7L, owner)));

        assertThat(taskService.findForOwner(7L, OWNER_ID).id()).isEqualTo(7L);
    }

    @Test
    void foreignOrMissingTaskIsReportedAsNotFound() {
        when(taskRepository.findByIdAndUserId(25L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findForOwner(25L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found");
    }

    @Test
    void updateModifiesOwnTask() {
        Task task = task(7L, owner);
        when(taskRepository.findByIdAndUserId(7L, OWNER_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        TaskResponse response = taskService.update(7L, OWNER_ID,
                new UpdateTaskRequest("New", "New desc", TaskStatus.COMPLETED, TaskPriority.LOW));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    void updateOfForeignTaskIsRejected() {
        when(taskRepository.findByIdAndUserId(25L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(25L, OWNER_ID,
                new UpdateTaskRequest("New", null, TaskStatus.COMPLETED, TaskPriority.LOW)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRemovesOwnTask() {
        Task task = task(7L, owner);
        when(taskRepository.findByIdAndUserId(7L, OWNER_ID)).thenReturn(Optional.of(task));

        taskService.delete(7L, OWNER_ID);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteOfForeignTaskIsRejected() {
        when(taskRepository.findByIdAndUserId(25L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(25L, OWNER_ID)).isInstanceOf(ResourceNotFoundException.class);
        verify(taskRepository, never()).delete(any());
    }
}
