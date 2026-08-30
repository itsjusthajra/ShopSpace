package com.shopspace.backend.service;

import com.shopspace.backend.dto.RegisterRequest;
import com.shopspace.backend.dto.UserResponse;
import com.shopspace.backend.entity.Role;
import com.shopspace.backend.entity.User;
import com.shopspace.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        return convertToResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public Optional<UserResponse> getUserById(Long id) {

        return userRepository.findById(id)
                .map(this::convertToResponse);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private UserResponse convertToResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled()
        );
    }
}