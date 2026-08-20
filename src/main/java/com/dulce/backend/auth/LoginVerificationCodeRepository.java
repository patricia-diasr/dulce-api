package com.dulce.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginVerificationCodeRepository
        extends JpaRepository<LoginVerificationCode, Long> {

    Optional<LoginVerificationCode> findFirstByCustomerIdAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(
            Long customerId, String code);
}
