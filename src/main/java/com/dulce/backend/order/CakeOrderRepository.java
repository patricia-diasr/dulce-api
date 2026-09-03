package com.dulce.backend.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CakeOrderRepository extends JpaRepository<CakeOrder, Long> {

    List<CakeOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
