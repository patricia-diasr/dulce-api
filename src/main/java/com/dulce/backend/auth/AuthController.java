package com.dulce.backend.auth;

import com.dulce.backend.auth.dto.AdminLoginRequest;
import com.dulce.backend.auth.dto.AuthResponse;
import com.dulce.backend.auth.dto.CustomerLoginRequest;
import com.dulce.backend.auth.dto.LoginCodeRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerLoginService customerLoginService;
    private final AdminLoginService adminLoginService;

    public AuthController(
            CustomerLoginService customerLoginService, AdminLoginService adminLoginService) {
        this.customerLoginService = customerLoginService;
        this.adminLoginService = adminLoginService;
    }

    @PostMapping("/customers/login/code")
    public ResponseEntity<Void> requestCustomerLoginCode(
            @Valid @RequestBody LoginCodeRequest request) {
        customerLoginService.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/customers/login")
    public ResponseEntity<AuthResponse> loginCustomer(
            @Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(customerLoginService.verifyCode(request.email(), request.code()));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminLoginService.login(request.email(), request.password()));
    }
}
