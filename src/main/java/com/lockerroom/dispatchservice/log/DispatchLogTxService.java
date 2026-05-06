package com.lockerroom.dispatchservice.log;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DispatchLogTxService {

    private final DispatchLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnsureResult ensurePending(String eventId, DispatchEventType eventType, DispatchChannel channel,
                                      Long targetUserId) {
        Optional<DispatchLog> existing = repository.findByEventIdAndChannel(eventId, channel);
        if (existing.isPresent()) {
            DispatchLog log = existing.get();
            if (log.getStatus() == DispatchStatus.SUCCESS || log.getStatus() == DispatchStatus.SKIPPED) {
                return EnsureResult.skipped(log.getId(), log.getStatus());
            }
            return EnsureResult.pending(log.getId());
        }
        DispatchLog created = repository.save(DispatchLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .channel(channel)
                .status(DispatchStatus.PENDING)
                .retryCount(0)
                .targetUserId(targetUserId)
                .build());
        return EnsureResult.pending(created.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long logId, String providerMessageId) {
        repository.findById(logId).ifPresent(l -> l.markSuccess(providerMessageId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(Long logId, String errorCode, String errorMessage) {
        repository.findById(logId).ifPresent(l -> l.markFailure(errorCode, errorMessage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSkipped(Long logId, String reason) {
        repository.findById(logId).ifPresent(l -> l.markSkipped(reason));
    }

    public record EnsureResult(Long logId, boolean alreadyDone, DispatchStatus existingStatus) {
        public static EnsureResult pending(Long logId) {
            return new EnsureResult(logId, false, DispatchStatus.PENDING);
        }

        public static EnsureResult skipped(Long logId, DispatchStatus status) {
            return new EnsureResult(logId, true, status);
        }
    }
}
