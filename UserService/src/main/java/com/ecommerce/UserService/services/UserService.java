package com.ecommerce.UserService.services;

import com.ecommerce.UserService.models.*;
import com.ecommerce.UserService.models.enums.UserRole;
import com.ecommerce.UserService.repositories.UserRepository;
import com.ecommerce.UserService.services.factory.UserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private UserFactory userFactory;
    @Autowired private PasswordEncoder passwordEncoder;

    private User getUserOrThrow(String token, Long id) {
        User user= userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return user;
    }
    // REGISTRATION
    @Transactional
    public User registerUser(UserRole role, Object userData) {
        User user = userFactory.createUser(role, userData);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return user;
    }

    // USER MANAGEMENT
    public Optional<User> getUserById(Long id, String token) {
        return userRepository.findById(id);
    }

    @Transactional
    public User updateUser(Long id, Object updatedData, String token) {
        User existingUser = getUserOrThrow(token, id);

        // If the data is a Map of fields
        if (!(updatedData instanceof Map)) {
            throw new IllegalArgumentException("Updated data is not a valid map.");
        }

        Map<String, Object> data = (Map<String, Object>) updatedData;

        if (existingUser.getRole().equalsIgnoreCase("CUSTOMER")) {
            updateCustomerProfile((CustomerProfile) existingUser, data);
        } else if (existingUser.getRole().equalsIgnoreCase("MERCHANT")) {
            updateMerchantProfile((MerchantProfile) existingUser, data);
        } else {
            throw new IllegalArgumentException("Mismatched profile types or invalid role.");
        }

        return userRepository.save(existingUser);
    }

    private void updateCustomerProfile(CustomerProfile existingCustomer, Map<String, Object> data) {
        if (data.containsKey("username")) {
            existingCustomer.setUsername((String) data.get("username"));
        }
        if (data.containsKey("email")) {
            existingCustomer.setEmail((String) data.get("email"));
        }
        if (data.containsKey("password")) {
            existingCustomer.setPassword(passwordEncoder.encode((String) data.get("password")));
        }
        if (data.containsKey("phoneNumber")) {
            existingCustomer.setPhoneNumber((String) data.get("phoneNumber"));
        }
        if (data.containsKey("shippingAddress")) {
            existingCustomer.setShippingAddress((String) data.get("shippingAddress"));
        }
    }

    private void updateMerchantProfile(MerchantProfile existingMerchant, Map<String, Object> data) {
        if (data.containsKey("username")) {
            existingMerchant.setUsername((String) data.get("username"));
        }
        if (data.containsKey("email")) {
            existingMerchant.setEmail((String) data.get("email"));
        }
        if (data.containsKey("password")) {
            existingMerchant.setPassword(passwordEncoder.encode((String) data.get("password")));
        }
        if (data.containsKey("storeName")) {
            existingMerchant.setStoreName((String) data.get("storeName"));
        }
        if (data.containsKey("storeAddress")) {
            existingMerchant.setStoreAddress((String) data.get("storeAddress"));
        }
    }

    @Transactional
    public void deleteUser(Long id, String token) {
        userRepository.deleteById(id);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

}
