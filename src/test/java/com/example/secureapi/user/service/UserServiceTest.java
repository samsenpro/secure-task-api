package com.example.secureapi.user.service;

import com.example.secureapi.exception.BusinessRuleException;
import com.example.secureapi.exception.ResourceNotFoundException;
import com.example.secureapi.user.dto.UpdateUserRequest;
import com.example.secureapi.user.dto.UserResponse;
import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.example.secureapi.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUserReturnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "jane@example.com", Role.USER)));

        UserResponse response = userService.getCurrentUser(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("jane@example.com");
    }

    @Test
    void getCurrentUserFailsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCurrentUserRenames() {
        User user = user(1L, "jane@example.com", Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = userService.updateCurrentUser(1L, new UpdateUserRequest(" Jane Smith ", null, null));

        assertThat(response.name()).isEqualTo("Jane Smith");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateCurrentUserChangesPasswordWhenCurrentPasswordMatches() {
        User user = user(1L, "jane@example.com", Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword1", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("new-hash");
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        userService.updateCurrentUser(1L, new UpdateUserRequest("Jane", "OldPassword1", "NewPassword1"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
    }

    @Test
    void updateCurrentUserRejectsWrongCurrentPassword() {
        User user = user(1L, "jane@example.com", Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.updateCurrentUser(
                1L, new UpdateUserRequest("Jane", "WrongPass1", "NewPassword1")))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateCurrentUserRejectsPasswordChangeWithoutCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "jane@example.com", Role.USER)));

        assertThatThrownBy(() -> userService.updateCurrentUser(
                1L, new UpdateUserRequest("Jane", null, "NewPassword1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void disableAndEnableToggleAccountState() {
        User user = user(2L, "bob@example.com", Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        assertThat(userService.disable(2L, 1L).enabled()).isFalse();
        assertThat(userService.enable(2L).enabled()).isTrue();
    }

    @Test
    void adminCannotDisableOrDeleteThemselves() {
        assertThatThrownBy(() -> userService.disable(1L, 1L)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> userService.delete(1L, 1L)).isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesExistingUser() {
        User user = user(2L, "bob@example.com", Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        userService.delete(2L, 1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteFailsForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
