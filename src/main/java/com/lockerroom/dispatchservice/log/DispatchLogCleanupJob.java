package com.lockerroom.dispatchservice.log;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchLogCleanupJob {

    static final Duration RETENTION = Duration.ofDays(90);

    private final DispatchLogRepository repository;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "dispatchLogCleanup", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void cleanup() {
        Instant cutoff = Instant.now().minus(RETENTION);
        long deleted = repository.deleteByCreatedAtBefore(cutoff);
        log.info("dispatch_logs cleanup: deleted={}, cutoff={}", deleted, cutoff);
    }
}
