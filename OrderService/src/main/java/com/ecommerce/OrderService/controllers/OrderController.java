package com.ecommerce.OrderService.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @GetMapping("/")
    public String Hello() {
        return "Hello From order controller!";
    }
}
