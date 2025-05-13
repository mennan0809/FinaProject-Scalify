package com.ecommerce.ProductService.controllers;

import com.ecommerce.ProductService.Dto.UserSessionDTO;
import com.ecommerce.ProductService.models.Product;
import com.ecommerce.ProductService.models.ProductReview;
import com.ecommerce.ProductService.models.enums.ProductCategory;
import com.ecommerce.ProductService.services.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    private boolean isMerchantUser(String token) {
        UserSessionDTO userSession = productService.getUserSessionFromToken(token);
        return userSession != null && "MERCHANT".equalsIgnoreCase(userSession.getRole());
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> addProduct(
            @RequestParam("category") ProductCategory category,
            @RequestBody Map<String, Object> product,
            @RequestHeader("Authorization") String authorizationHeader) {

        try {
            String token = extractToken(authorizationHeader);
            UserSessionDTO userSession = productService.getUserSessionFromToken(token);
            if (userSession == null || !"MERCHANT".equals(userSession.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: Only merchants can add products.");
            }

            Product newProduct = productService.createProduct(userSession.getUserId(), category, product);
            return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/getToken")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String token) {
        try {
            String actualToken = extractToken(token);
            String response = productService.getToken(actualToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct,
            @RequestHeader("Authorization") String authorizationHeader) {

        try {
            String token = extractToken(authorizationHeader);
            UserSessionDTO userSession = productService.getUserSessionFromToken(token);
            if (userSession == null || !"MERCHANT".equals(userSession.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: Only merchants can update products.");
            }

            Product product = productService.updateProduct(userSession.getUserId(), userSession.getEmail(), id, updatedProduct);
            return ResponseEntity.ok(product);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id,
                                           @RequestHeader("Authorization") String authorizationHeader) {
        try {
            String token = extractToken(authorizationHeader);
            UserSessionDTO userSession = productService.getUserSessionFromToken(token);
            if (userSession == null || !"MERCHANT".equals(userSession.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: Only merchants can delete products.");
            }

            productService.deleteProduct(userSession.getUserId(), id);
            return ResponseEntity.ok("Product deleted successfully.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/addstock")
    public ResponseEntity<?> addStock(
            @PathVariable Long id,
            @RequestParam int stock,
            @RequestHeader("Authorization") String authorizationHeader) {

        try {
            String token = extractToken(authorizationHeader);
            UserSessionDTO userSession = productService.getUserSessionFromToken(token);
            if (userSession == null || !"MERCHANT".equals(userSession.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: Only merchants can add stock.");
            }

            productService.addStock(userSession.getEmail(), id, stock);
            return ResponseEntity.ok("Stock added successfully.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/removestock")
    public ResponseEntity<?> removeStock(
            @PathVariable Long id,
            @RequestParam int stock,
            @RequestHeader("Authorization") String authorizationHeader) {

        try {
            String token = extractToken(authorizationHeader);
            UserSessionDTO userSession = productService.getUserSessionFromToken(token);
            if (userSession == null || !"MERCHANT".equals(userSession.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized: Only merchants can remove stock.");
            }

            productService.removeStock(userSession.getEmail(), id, stock);
            return ResponseEntity.ok("Stock removed successfully.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
