package com.ecommerce.PaymentService.services.Factory;


import com.ecommerce.PaymentService.clients.UserServiceClient;
import com.ecommerce.PaymentService.dto.UserDto;
import com.ecommerce.PaymentService.models.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentStrategyFactory {

    private UserServiceClient userServiceClient;

    @Autowired
    public PaymentStrategyFactory(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }
    public String createPaymentStrategy(Long userId,String token,PaymentMethod method, Object... params) {

        switch (method) {
            case CREDIT_CARD:
                return "CreditCard";
            case WALLET:
                return "Wallet";
            default:
                throw new IllegalArgumentException("Unsupported payment method");
        }
    }
}