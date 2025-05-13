package com.ecommerce.OrderService.services;

import com.ecommerce.OrderService.Dto.*;
import com.ecommerce.OrderService.models.Cart;
import com.ecommerce.OrderService.models.CartItem;
import com.ecommerce.OrderService.models.Order;
import com.ecommerce.OrderService.models.enums.OrderStatus;
import com.ecommerce.OrderService.repositories.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class OrderService {

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_MERCHANT = "MERCHANT";
    private static final String ROLE_ADMIN = "ADMIN";

    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    private UserSessionDTO getSession(String token) {
        //get from cache
        UserSessionDTO session = null;
        if (session == null) throw new RuntimeException("Session not found for token: " + token);
        return session;
    }

    private Cart getCart(String token) {
        //get from cache
        Cart cart = null;
        if (cart == null) throw new RuntimeException("Cart not found for token: " + token);
        return cart;
    }

    @Transactional
    public void createOrder(String token, Long transactionId) {
        UserSessionDTO userSessionDTO = getSession(token);
        if (!userSessionDTO.getRole().equals(ROLE_CUSTOMER)) {
            throw new RuntimeException("Unauthorized role");
        }
        Cart cart = getCart(token);

        Map<Long, List<CartItem>> itemsByMerchant = new HashMap<>();
        cart.getItems().values().forEach(item ->
                itemsByMerchant.computeIfAbsent(item.getMerchantId(), k -> new ArrayList<>()).add(item)
        );

        itemsByMerchant.forEach((merchantId, orderProducts) -> {
            Order order = new Order();
            order.setUserId(cart.getUserId());
            order.setMerchantId(merchantId);
            order.setOrderProducts(orderProducts);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setTotalPrice(calculateTotalPrice(orderProducts));
            order.setTotalItemCount(orderProducts.size());
            order.setUserEmail(cart.getUserEmail());
            order.setTransactionId(transactionId);
            orderRepository.save(order);
        });

        cartService.clearCart(token);
    }

    private double calculateTotalPrice(List<CartItem> items) {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }

    public Order getOrderById(String token, Long orderId) {
        UserSessionDTO userSessionDTO = getSession(token);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        switch (userSessionDTO.getRole()) {
            case ROLE_CUSTOMER:
                if (order.getUserId().equals(userSessionDTO.getUserId())) return order;
            case ROLE_MERCHANT:
                if (order.getMerchantId().equals(userSessionDTO.getUserId())) return order;
            case ROLE_ADMIN:
                return order;
            default:
                throw new RuntimeException("You don't have permission to access this order");
        }

    }

    public Order updateOrder(String token, Long orderId, Order updatedOrder) {
        UserSessionDTO userSessionDTO = getSession(token);
        if (!userSessionDTO.getRole().equals(ROLE_MERCHANT) || !userSessionDTO.getRole().equals(ROLE_ADMIN)) {
            throw new RuntimeException("You are not allowed to update this order");
        }
        Order existingOrder = getOrderById(token, orderId);
        boolean statusChanged = !existingOrder.getStatus().equals(updatedOrder.getStatus());

        existingOrder.setStatus(updatedOrder.getStatus());
        existingOrder.setOrderProducts(updatedOrder.getOrderProducts());
        existingOrder.setTotalPrice(updatedOrder.getTotalPrice());
        existingOrder.setTotalItemCount(updatedOrder.getTotalItemCount());

        Order savedOrder = orderRepository.save(existingOrder);

        return savedOrder;
    }

    public void deleteOrder(String token, Long orderId) {
        UserSessionDTO userSessionDTO = getSession(token);
        if (!userSessionDTO.getRole().equals(ROLE_MERCHANT) || !userSessionDTO.getRole().equals(ROLE_ADMIN)) {
            throw new RuntimeException("You are not allowed to delete this order");
        }
        Order order = getOrderById(token, orderId);
        orderRepository.delete(order);
    }

    public List<Order> getAllOrders(String token) {
        UserSessionDTO session = getSession(token);
        switch (session.getRole().toUpperCase()) {
            case "MERCHANT":
                return orderRepository.findByMerchantId(session.getUserId());
            case "CUSTOMER":
                return orderRepository.findByUserId(session.getUserId());

            case "ADMIN":
                return orderRepository.findAll();

            default:
                throw new IllegalArgumentException("Invalid user role: " + session.getRole());
        }
    }

}