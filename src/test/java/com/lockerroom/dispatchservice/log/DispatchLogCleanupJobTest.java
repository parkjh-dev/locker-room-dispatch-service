package com.lockerroom.dispatchservice.log;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchLogCleanupJobTest {

    @Mock
    DispatchLogRepository repository;

    @InjectMocks
    DispatchLogCleanupJob job;

    @Test
    void cleanup_callsDeleteWith90DayCutoff() {
        when(repository.deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(42L);

        Instant before = Instant.now();
        job.cleanup();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByCreatedAtBefore(captor.capture());
        Instant cutoff = captor.getValue();

        assertThat(cutoff)
                .isAfterOrEqualTo(before.minus(DispatchLogCleanupJob.RETENTION).minusSeconds(1))
                .isBeforeOrEqualTo(after.minus(DispatchLogCleanupJob.RETENTION).plusSeconds(1));
    }
}
