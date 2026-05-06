package com.lockerroom.dispatchservice.log;

import com.lockerroom.dispatchservice.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "dispatch_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dispatch_logs_event_channel",
                columnNames = {"event_id", "channel"}
        )
)
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DispatchLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private DispatchEventType eventType;

    @Column(name = "channel", length = 32)
    @Enumerated(EnumType.STRING)
    private DispatchChannel channel;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DispatchStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "target_user_id")
    private Long targetUserId;

    public void markSuccess(String providerMessageId) {
        this.status = DispatchStatus.SUCCESS;
        this.providerMessageId = providerMessageId;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markFailure(String errorCode, String errorMessage) {
        this.status = DispatchStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = truncate(errorMessage, 1000);
        this.retryCount += 1;
    }

    public void markSkipped(String reason) {
        this.status = DispatchStatus.SKIPPED;
        this.errorMessage = truncate(reason, 1000);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
