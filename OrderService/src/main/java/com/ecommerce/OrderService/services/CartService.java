package com.ecommerce.OrderService.services;

import com.ecommerce.OrderService.Clients.ProductServiceFeignClient;
import com.ecommerce.OrderService.Dto.UserSessionDTO;
import com.ecommerce.OrderService.models.Cart;
import com.ecommerce.OrderService.models.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final ProductServiceFeignClient productServiceFeignClient;

    @Autowired
    public CartService(
            ProductServiceFeignClient productServiceFeignClient
    ) {
        this.productServiceFeignClient = productServiceFeignClient;
    }


    public UserSessionDTO getSession(String token) {
        // Fetch the session for the token
        //get from cache
        UserSessionDTO session = null;
        if (session == null) {
            throw new RuntimeException("Session not found in cache for token: " + token);
        }
        else if(!session.getRole().equalsIgnoreCase("CUSTOMER")){
            throw new RuntimeException("You are not a customer");
        }
        return session;
    }

    public void addItemToCart(String token, Long productId, int quantity) {
        UserSessionDTO session = getSession(token);
        if(!session.getRole().equalsIgnoreCase(ROLE_CUSTOMER)){
            throw new RuntimeException("You are not a customer");
        }
        // Check product availability
        var product = productServiceFeignClient.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("Product not found");
        } else if (product.getStockLevel() < quantity) {
            throw new RuntimeException("Not Enough Stock"+product.getStockLevel() +"less than"+ quantity);
        }
        Long userId = session.getUserId();
        String email = session.getEmail();

        //get from cache
        Cart cart = null;
        if (cart == null) {
            cart = new Cart(userId, email);
        } else {
            cart.setUserEmail(email);
        }

        // Add the item to the cart
        cart.addItem(productId, quantity, product.getPrice(), product.getMerchantId());

        // Save the updated cart back to Redis
        //cache
    }

    public Cart viewCart(String token) {
        UserSessionDTO session = getSession(token);
        //cache
        Cart cart = null;
        return cart != null ? cart : new Cart(session.getUserId(), session.getEmail());
    }

    public void removeItemFromCart(String token, Long productId, Integer quantity) {
        UserSessionDTO session = getSession(token);
        //cache
        Cart cart = null;

        if (cart != null) {
            if (quantity == null) {
                // Remove entire item if no quantity specified
                cart.removeItem(productId);
            } else {
                // Reduce quantity
                CartItem item = cart.getItems().get(productId);
                if (item != null) {
                    int newQuantity = item.getQuantity() - quantity;
                    if (newQuantity <= 0) {
                        // If quantity after removal <= 0, remove the whole item
                        cart.removeItem(productId);
                    } else {
                        // Else, update quantity and adjust totals
                        item.setQuantity(newQuantity);
                        // Update the cart total manually
                        cart.recalculateTotals();
                    }
                }
            }
            // Save updated cart back to Redis
            //cache
        } else {
            throw new RuntimeException("Cart not found for token: " + token);
        }
    }


    public void clearCart(String token) {
        // Delete the cart from Redis
        UserSessionDTO session = getSession(token);
        //cache
    }
}
