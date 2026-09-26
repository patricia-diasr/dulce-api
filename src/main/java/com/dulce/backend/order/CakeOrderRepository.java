package com.dulce.backend.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CakeOrderRepository
        extends JpaRepository<CakeOrder, Long>, JpaSpecificationExecutor<CakeOrder> {

    List<CakeOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
