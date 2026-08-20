package com.dulce.backend.customer;

import com.dulce.backend.customer.dto.CustomerRegistrationRequest;
import com.dulce.backend.customer.dto.CustomerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse register(CustomerRegistrationRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Já existe um cliente cadastrado com este e-mail.");
        }

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        Customer saved = customerRepository.save(customer);

        return new CustomerResponse(
                saved.getId(), saved.getName(), saved.getEmail(), saved.getPhone());
    }
}
