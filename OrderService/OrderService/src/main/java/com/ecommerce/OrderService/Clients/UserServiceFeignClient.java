package com.ecommerce.OrderService.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "http://user-service:8080")
public interface UserServiceFeignClient {
    @PostMapping("/deposit/{userId}")
    void deposit(@RequestHeader("Authorization") String token,
                 @PathVariable("userId") Long userId,
                 @RequestBody Double amount);
}