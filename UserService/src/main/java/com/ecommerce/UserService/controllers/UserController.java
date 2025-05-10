package com.ecommerce.UserService.controllers;

import com.ecommerce.UserService.models.User;
import com.ecommerce.UserService.models.enums.UserRole;
import com.ecommerce.UserService.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Helper method to extract the token from the Authorization header
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);  // Remove "Bearer " prefix
        }
        return null;  // If the header doesn't contain a Bearer token, return null
    }


    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestParam("role") UserRole role, @RequestBody Object userData) {
        User user = userService.registerUser(role, userData);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        Optional<User> user = userService.getUserById(id, token);
        return user.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody Object userData, @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        User updated = userService.updateUser(id, userData, token);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        userService.deleteUser(id, token);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/reset")
    public void resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        boolean isVerified = userService.verifyEmail(token);
        if (isVerified) {
            return ResponseEntity.ok("Email verified successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token.");
        }
    }
}
