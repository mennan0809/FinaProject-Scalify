package com.ecommerce.OrderService.services.command;

import com.ecommerce.OrderService.models.Order;
import com.ecommerce.OrderService.models.enums.OrderStatus;
import com.ecommerce.OrderService.repositories.OrderRepository;

public class RefundOrderCommand extends OrderCommand {

    private final OrderRepository orderRepository;

    public RefundOrderCommand(Order order, OrderRepository orderRepository) {
        super(order);
        this.orderRepository = orderRepository;
    }

    @Override
    public void execute() {
        // Process refund logic here (for example, interact with a payment gateway)
        System.out.println("Refund Order Command");
        System.out.println(order.getStatus());
        if(!order.getStatus().equals(OrderStatus.REFUND_PENDING)) {
            throw new RuntimeException("Order can't Be Refunded");
        }
        // Update order status to REFUNDED
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }
}
