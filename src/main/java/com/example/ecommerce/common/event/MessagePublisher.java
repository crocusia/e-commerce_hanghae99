package com.example.ecommerce.common.event;

public interface MessagePublisher {
    void publish(DomainEvent event);
}