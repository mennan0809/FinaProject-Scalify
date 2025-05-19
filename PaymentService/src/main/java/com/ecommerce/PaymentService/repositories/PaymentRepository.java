package com.ecommerce.PaymentService.repositories;


import com.ecommerce.PaymentService.models.Payment;
import com.ecommerce.PaymentService.models.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);
    List<Payment> findAll();
}