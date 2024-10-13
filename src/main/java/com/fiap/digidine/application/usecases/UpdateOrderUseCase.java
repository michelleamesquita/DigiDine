package com.fiap.digidine.application.usecases;

import com.fiap.digidine.application.gateways.OrderGateway;
import com.fiap.digidine.domain.entities.Order;

public class UpdateOrderUseCase {

    private final OrderGateway ordersGateway;

    public UpdateOrderUseCase(OrderGateway ordersGateway) {
        this.ordersGateway = ordersGateway;
    }

    public Order updateOrder(String id, Order order){
        return ordersGateway.updateOrder(id, order);
    }

}
