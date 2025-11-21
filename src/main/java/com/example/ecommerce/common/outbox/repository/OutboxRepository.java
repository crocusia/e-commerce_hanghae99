package com.example.ecommerce.common.outbox.repository;

import com.example.ecommerce.common.outbox.domain.Outbox;
import com.example.ecommerce.common.outbox.domain.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    Optional<Outbox> findByEventId(String eventId);

    List<Outbox> findByStatus(OutboxStatus status);

    List<Outbox> findByStatusAndCreatedAtBefore(
        OutboxStatus status,
        LocalDateTime createdAt
    );

    Outbox save(Outbox outbox);
}
