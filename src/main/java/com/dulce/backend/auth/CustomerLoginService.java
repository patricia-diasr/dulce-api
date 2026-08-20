package com.dulce.backend.auth;

import com.dulce.backend.auth.dto.AuthResponse;
import com.dulce.backend.customer.Customer;
import com.dulce.backend.customer.CustomerRepository;
import com.dulce.backend.notification.LoginCodeNotifier;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerLoginService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final LoginVerificationCodeRepository codeRepository;
    private final LoginCodeNotifier loginCodeNotifier;
    private final JwtService jwtService;
    private final long codeExpirationMinutes;

    public CustomerLoginService(
            CustomerRepository customerRepository,
            LoginVerificationCodeRepository codeRepository,
            LoginCodeNotifier loginCodeNotifier,
            JwtService jwtService,
            @Value("${app.verification-code.expiration-minutes}") long codeExpirationMinutes) {
        this.customerRepository = customerRepository;
        this.codeRepository = codeRepository;
        this.loginCodeNotifier = loginCodeNotifier;
        this.jwtService = jwtService;
        this.codeExpirationMinutes = codeExpirationMinutes;
    }

    @Transactional
    public void requestCode(String email) {
        customerRepository
                .findByEmail(email)
                .ifPresent(
                        customer -> {
                            String code = generateCode();

                            LoginVerificationCode verification = new LoginVerificationCode();
                            verification.setCustomer(customer);
                            verification.setCode(code);
                            verification.setExpiresAt(
                                    OffsetDateTime.now().plusMinutes(codeExpirationMinutes));
                            verification.setCreatedAt(OffsetDateTime.now());
                            codeRepository.save(verification);

                            loginCodeNotifier.notifyLoginCode(
                                    customer.getName(), customer.getEmail(), code);
                        });
    }

    @Transactional
    public AuthResponse verifyCode(String email, String code) {
        Customer customer =
                customerRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        LoginVerificationCode verification =
                codeRepository
                        .findFirstByCustomerIdAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(
                                customer.getId(), code)
                        .orElseThrow(InvalidCredentialsException::new);

        if (verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidCredentialsException();
        }

        verification.setUsedAt(OffsetDateTime.now());
        codeRepository.save(verification);

        return jwtService.generateAuthResponse(
                customer.getEmail(), Role.CUSTOMER, customer.getId());
    }

    private String generateCode() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }
}
