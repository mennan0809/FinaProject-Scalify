package com.ecommerce.PaymentService.controllers;

import com.ecommerce.PaymentService.dto.UserSessionDTO;
import com.ecommerce.PaymentService.models.Payment;
import com.ecommerce.PaymentService.models.PaymentRequest;
import com.ecommerce.PaymentService.models.enums.PaymentMethod;
import com.ecommerce.PaymentService.models.enums.PaymentStatus;
import com.ecommerce.PaymentService.services.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;

    }

    @GetMapping("/getToken")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String token) {
        try {
            String actualToken = extractToken(token);
            String response = paymentService.getToken(actualToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return new ResponseEntity<>("Error getting token: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    // Create payment
    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestParam Long userId,
            @RequestParam String customerEmail,
            @RequestParam PaymentMethod method,
            @RequestParam double amount,
            @RequestBody(required = false) PaymentRequest paymentRequest, // Make it optional
            @RequestParam String token) {

        try {
            // Get user session details from the token
            UserSessionDTO userSession = paymentService.getUserSessionFromToken(token);
            if (userSession == null || !"CUSTOMER".equals(userSession.getRole())) {
                return new ResponseEntity<>("Unauthorized: Invalid user session", HttpStatus.BAD_REQUEST);
            }

            String[] paymentDetails = paymentRequest != null ? paymentRequest.getPaymentDetails() : new String[0];

            Payment payment = paymentService.processPayment(token, userId, customerEmail, method, amount, paymentDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating payment: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @PutMapping("/{id}/status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam PaymentStatus status) {
        try {
            Payment payment = paymentService.updatePaymentStatus(id, status);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating payment status: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        try {
            paymentService.deletePayment(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting payment: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get payment by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPayment(@PathVariable Long id) {
        try {
            Payment payment = paymentService.getPaymentById(id);
            return payment != null ? ResponseEntity.ok(payment) : new ResponseEntity<>("Payment not found", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving payment: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get user payments
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPayments(@PathVariable Long userId) {
        try {
            List<Payment> payments = paymentService.getUserPayments(userId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving user payments: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        try {
            List<Payment> payments = paymentService.getPaymentsByStatus(status);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving payments by status: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}

