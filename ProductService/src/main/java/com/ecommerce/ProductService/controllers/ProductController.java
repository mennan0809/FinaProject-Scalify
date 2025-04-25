package com.ecommerce.ProductService.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {
    @GetMapping("/")
    public String Hello() {
        return "Hello from ProductController";
    }
}
