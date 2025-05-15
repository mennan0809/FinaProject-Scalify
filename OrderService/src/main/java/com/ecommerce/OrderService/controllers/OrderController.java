package com.ecommerce.OrderService.controllers;

import com.ecommerce.OrderService.models.Order;
import com.ecommerce.OrderService.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }


    // POST: Create a new order
    @PostMapping("/checkout")
    public ResponseEntity<String> makeOrder(@RequestHeader("Authorization") String token, @RequestParam Long transactionId) {
        try {
            orderService.createOrder(extractToken(token), transactionId);
            return ResponseEntity.status(HttpStatus.CREATED).body("Order created successfully.");
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating order: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // GET: Read an order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@RequestHeader("Authorization") String token, @PathVariable Long orderId) {
        try {
            Order order = orderService.getOrderById(extractToken(token), orderId);
            return ResponseEntity.status(HttpStatus.OK).body(order);
        } catch (Exception e) {
            return new ResponseEntity<>("Error fetching order: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // PUT: Update an existing order
    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long orderId,
            @RequestBody Order updatedOrder) {
        try {
            Order order = orderService.updateOrder(extractToken(token), orderId, updatedOrder);
            return ResponseEntity.status(HttpStatus.OK).body(order);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating order: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // DELETE: Delete an order by ID
    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> deleteOrder(@RequestHeader("Authorization") String token, @PathVariable Long orderId) {
        try {
            orderService.deleteOrder(extractToken(token), orderId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Order deleted successfully.");
        } catch (Exception e) {
            return new ResponseEntity<>("Error deleting order: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // GET: List all orders (Optional, might be useful for admins)
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String token) {
        try {
            Iterable<Order> orders = orderService.getAllOrders(extractToken(token));
            return ResponseEntity.status(HttpStatus.OK).body(orders);
        } catch (Exception e) {
            return new ResponseEntity<>("Error fetching all orders: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
