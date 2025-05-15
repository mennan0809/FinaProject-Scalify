package com.ecommerce.PaymentService.services;
import com.ecommerce.PaymentService.clients.UserServiceClient;
import com.ecommerce.PaymentService.dto.UserDto;
import com.ecommerce.PaymentService.dto.UserSessionDTO;
import com.ecommerce.PaymentService.models.Payment;
import com.ecommerce.PaymentService.models.enums.PaymentMethod;
import com.ecommerce.PaymentService.models.enums.PaymentStatus;
import com.ecommerce.PaymentService.repositories.PaymentRepository;
import com.ecommerce.PaymentService.services.Factory.PaymentStrategyFactory;
import com.ecommerce.PaymentService.services.strategy.PaymentStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    @Autowired
    private MailService mailService;
    private final RedisTemplate<String, UserSessionDTO> sessionRedisTemplate;
    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, RedisTemplate<String, UserSessionDTO> sessionRedisTemplate, UserServiceClient userServiceClient) {
        this.paymentRepository = paymentRepository;
        this.sessionRedisTemplate = sessionRedisTemplate;
        this.userServiceClient = userServiceClient;
    }

    public String getToken(String token) {
        UserSessionDTO userSession=sessionRedisTemplate.opsForValue().get(token);
        assert userSession != null;
        return userSession.toString();
    }
    public UserSessionDTO getUserSessionFromToken(String token) {
        // Retrieve the user session from Redis using the provided token
        UserSessionDTO userSession=sessionRedisTemplate.opsForValue().get(token);

        // Check if the session exists and if the user role is "merchant"
        return userSession;
    }
    @Transactional
    public Payment processPayment(String token, Long userId, String customerEmail,
                                  PaymentMethod method, double amount, Object... paymentDetails) {
        UserDto user = userServiceClient.getUser(userId, "Bearer " + token);
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setCustomerEmail(customerEmail);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionDate(LocalDateTime.now());

        // Save initially as PENDING
        payment = paymentRepository.save(payment);

        try {
            PaymentStrategy strategy = paymentStrategyFactory.createPaymentStrategy(userId, token, method, paymentDetails);

            boolean isSuccessful = strategy.processPayment(amount);

            payment.setStatus(isSuccessful ? PaymentStatus.SUCCESSFUL : PaymentStatus.FAILED);
            payment.setTransactionId(UUID.randomUUID().toString());

            if (payment.getStatus() == PaymentStatus.FAILED) {
                mailService.sendEmail(user.getEmail(),
                        "Payment Failed",
                        "Dear Customer,\n\n"
                                + "We regret to inform you that your payment of $" + amount + " could not be processed successfully.\n"
                                + "Transaction ID: " + payment.getTransactionId() + "\n\n"
                                + "Please ensure you have sufficient balance or try again later. If the issue persists, contact support.\n\n"
                                + "Thank you,\n"
                                + "E-commerce Support Team"

                );
            }
            if (isSuccessful) {
                mailService.sendEmail(
                        user.getEmail(),
                        "Payment Successful",
                        "Dear Customer,\n\n"
                                + "Your payment of $" + amount + " has been successfully processed.\n"
                                + "Transaction ID: " + payment.getTransactionId() + "\n\n"
                                + "Thank you for your purchase! If you have any questions or need further assistance, feel free to contact our support team.\n\n"
                                + "Best regards,\n"
                                + "E-commerce Support Team"

                );

            }


            return paymentRepository.save(payment);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing failed", e);
        }
    }


    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(newStatus);
        return paymentRepository.save(payment);
    }
    @Transactional
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        paymentRepository.delete(payment);
    }
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }
    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

}
