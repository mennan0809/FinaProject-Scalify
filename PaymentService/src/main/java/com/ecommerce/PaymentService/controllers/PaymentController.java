package com.ecommerce.PaymentService.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    @GetMapping("/")
    public String Hello() {
        return "Hello From payment controller!";
    }
}

