package com.ecommerce.ProductService.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://user-service:8080")
public interface UserServiceFeignClient {
    @GetMapping("users/email/{id}")
    String getUserEmailById(@PathVariable("id") Long id);
}

