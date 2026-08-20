package com.dulce.backend.auth;

import com.dulce.backend.auth.dto.AuthResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminLoginService {

    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminLoginService(
            AdministratorRepository administratorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(String email, String rawPassword) {
        Administrator admin =
                administratorRepository
                        .findByEmail(email)
                        .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.generateAuthResponse(admin.getEmail(), Role.ADMIN, admin.getId());
    }
}
