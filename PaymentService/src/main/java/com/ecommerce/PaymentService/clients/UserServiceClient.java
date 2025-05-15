package com.ecommerce.PaymentService.clients;

import com.ecommerce.PaymentService.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://user-service:8080/users")
public interface UserServiceClient {


    @GetMapping("/{id}")
    UserDto getUser(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);
}
