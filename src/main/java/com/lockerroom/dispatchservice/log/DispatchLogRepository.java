package com.lockerroom.dispatchservice.log;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface DispatchLogRepository extends JpaRepository<DispatchLog, Long> {

    Optional<DispatchLog> findByEventIdAndChannel(String eventId, DispatchChannel channel);

    boolean existsByEventIdAndChannel(String eventId, DispatchChannel channel);

    @Modifying
    @Transactional
    long deleteByCreatedAtBefore(Instant cutoff);
}
