package com.fiap.digidine.infrastructure.gateways;

import com.fiap.digidine.application.gateways.OrderGateway;
import com.fiap.digidine.domain.entities.Customer;
import com.fiap.digidine.domain.entities.Order;
import com.fiap.digidine.domain.entities.Product;
import com.fiap.digidine.domain.entities.enums.OrderStatus;
import com.fiap.digidine.infrastructure.gateways.mappers.CustomerEntityMapper;
import com.fiap.digidine.infrastructure.gateways.mappers.OrderEntityMapper;
import com.fiap.digidine.infrastructure.persistence.OrderMongoDBRepository;
import com.fiap.digidine.infrastructure.persistence.entities.mongodb.OrderEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderRepositoryGateway implements OrderGateway {

    private final OrderMongoDBRepository orderRepository;
    private final OrderEntityMapper orderEntityMapper;

    public OrderRepositoryGateway(OrderMongoDBRepository ordersRepository, OrderEntityMapper orderEntityMapper) {
        this.orderRepository = ordersRepository;
        this.orderEntityMapper = orderEntityMapper;
    }


    @Override
    public String createOrder(Order order) {
        Order baseOrder = new Order();

        baseOrder.setOrderStatus(OrderStatus.RECEBIDO);
        baseOrder.setCreatedAt(LocalDateTime.now());
        baseOrder.setOrderNumber(UUID.randomUUID().toString());
        baseOrder.setCustomer(order.getCustomer());
        baseOrder.setProducts(order.getProducts());
        baseOrder.setTotalPrice(calculatePrice(order.getProducts()));

        orderEntityMapper.toDomain(orderRepository.save(orderEntityMapper.toEntity(baseOrder)));

        return baseOrder.getOrderNumber();
    }

    @Override
    public Order updateOrderStatus(String orderNumber, OrderStatus status) {
        OrderEntity orderEntity = orderRepository.findByOrderNumber(orderNumber);

        if(orderEntity == null) {
            throw new IllegalArgumentException("Pedido não cadastrado anteriormente!");
        }

        orderEntity.setStatus(status);

        // Salvando a entidade atualizada
        orderRepository.save(orderEntity);

        return orderEntityMapper.toDomain(orderEntity);
    }

    @Override
    public Order updateOrder(String orderNumber, Order order) {
        OrderEntity orderEntity = orderRepository.findByOrderNumber(orderNumber);

        if(orderEntity == null) {
            throw new IllegalArgumentException("Pedido não cadastrado anteriormente!");
        }

        // Atualizando os dados do pedido
        Order existingOrder = orderEntityMapper.toDomain(orderEntity);
        existingOrder.setCustomer(order.getCustomer());
        existingOrder.setProducts(order.getProducts());
        existingOrder.setTotalPrice(calculatePrice(order.getProducts()));

        orderRepository.save(orderEntityMapper.toEntity(existingOrder));

        return existingOrder;
    }

    @Override
    public List<Order> listOrders() {
        List<OrderEntity> orderEntities = orderRepository
                .findByStatusNotOrderByStatusAscCreatedAtAsc("Finalizado");

        if (orderEntities == null || orderEntities.isEmpty()) {
            throw new IllegalArgumentException();
        }

        // Mapeamento para objetos de domínio
        List<Order> orders = orderEntityMapper.toDomains(orderEntities);

        // Ordenação customizada dos status
        orders.sort(Comparator.comparingInt(this::getStatusPriority)
                .thenComparing(Order::getCreatedAt));

        return orders;
    }

    /*
     * @return Retorna a prioridade de status de um pedido
     * @param order Pedido a ser verificado
     */
    private int getStatusPriority(Order order) {
        switch (order.getOrderStatus()) {
            case OrderStatus.PRONTO: return 1;
            case OrderStatus.EM_PREPARACAO: return 2;
            case OrderStatus.RECEBIDO: return 3;
            default: return Integer.MAX_VALUE;
        }
    }

    @Override
    public String checkoutOrder(List<Product> products, Customer customer) {
        Order order = new Order(UUID.randomUUID().toString(), customer, products, calculatePrice(products), OrderStatus.RECEBIDO, LocalDateTime.now());
        createOrder(order);
        return order.getOrderNumber();
    }

    /*
     * @return Retorna o preço total dos produtos
     * @param products Lista de produtos
     */
    @Override
    public Double calculatePrice(List<Product> products) {
        double totalPrice = 0.0;

        for (Product product : products) {
            totalPrice += product.getPrice();
        }
        return totalPrice;
    }

    @Override
    public OrderStatus getOrderStatus(String orderNumber) {
        OrderEntity orderEntity = orderRepository.findByOrderNumber(orderNumber);

        if(orderEntity == null) {
            throw new IllegalArgumentException("Pedido não cadastrado anteriormente!");
        }

        Order order = orderEntityMapper.toDomain(orderEntity);

        return order.getOrderStatus();
    }
}