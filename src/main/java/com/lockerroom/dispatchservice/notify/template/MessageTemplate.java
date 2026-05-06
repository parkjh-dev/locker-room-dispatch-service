package com.lockerroom.dispatchservice.notify.template;

import com.lockerroom.dispatchservice.common.entity.BaseEntity;
import com.lockerroom.dispatchservice.log.DispatchChannel;
import com.lockerroom.dispatchservice.log.DispatchEventType;

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
        name = "message_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_message_templates_type_channel_version",
                columnNames = {"event_type", "channel", "version"}
        )
)
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private DispatchEventType eventType;

    @Column(name = "channel", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DispatchChannel channel;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "variables_json", columnDefinition = "TEXT")
    private String variablesJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;
}
