package com.dulce.backend.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByEmail(String email);
}
