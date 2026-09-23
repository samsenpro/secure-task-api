package com.example.secureapi.user.service;

import com.example.secureapi.common.PageResponse;
import com.example.secureapi.exception.BusinessRuleException;
import com.example.secureapi.exception.ResourceNotFoundException;
import com.example.secureapi.user.dto.UpdateUserRequest;
import com.example.secureapi.user.dto.UserResponse;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Perfil propio ---

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long currentUserId) {
        return UserResponse.from(findUser(currentUserId));
    }

    @Transactional
    public UserResponse updateCurrentUser(Long currentUserId, UpdateUserRequest request) {
        User user = findUser(currentUserId);
        user.rename(request.name().trim());

        if (request.wantsPasswordChange()) {
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BusinessRuleException("Current password is incorrect");
            }
            user.changePassword(passwordEncoder.encode(request.newPassword()));
            log.info("User id={} changed their password", user.getId());
        }

        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    // --- Administración ---

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse enable(Long id) {
        User user = findUser(id);
        user.enable();
        log.info("User id={} enabled", id);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse disable(Long id, Long currentAdminId) {
        preventSelfAction(id, currentAdminId, "Administrators cannot disable their own account");
        User user = findUser(id);
        user.disable();
        log.info("User id={} disabled by admin id={}", id, currentAdminId);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id, Long currentAdminId) {
        preventSelfAction(id, currentAdminId, "Administrators cannot delete their own account");
        User user = findUser(id);
        // Las tareas del usuario se eliminan en cascada (ON DELETE CASCADE).
        userRepository.delete(user);
        log.info("User id={} deleted by admin id={}", id, currentAdminId);
    }

    private void preventSelfAction(Long targetId, Long currentAdminId, String message) {
        if (targetId.equals(currentAdminId)) {
            throw new BusinessRuleException(message);
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
