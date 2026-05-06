-- ============================================================================
-- dispatch-service initial schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS dispatch_logs (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    event_id             VARCHAR(64)  NOT NULL,
    event_type           VARCHAR(64)  NOT NULL,
    channel              VARCHAR(32)  NULL,
    status               VARCHAR(32)  NOT NULL,
    retry_count          INT          NOT NULL DEFAULT 0,
    provider_message_id  VARCHAR(128) NULL,
    error_code           VARCHAR(64)  NULL,
    error_message        VARCHAR(1000) NULL,
    target_user_id       BIGINT       NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispatch_logs_event_channel (event_id, channel),
    KEY idx_dispatch_logs_event_type_status (event_type, status),
    KEY idx_dispatch_logs_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS message_templates (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(64) NOT NULL,
    channel         VARCHAR(32) NOT NULL,
    version         INT         NOT NULL DEFAULT 1,
    subject         VARCHAR(200) NULL,
    body            TEXT        NOT NULL,
    variables_json  TEXT        NULL,
    enabled         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_templates_type_channel_version (event_type, channel, version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shedlock (
    name        VARCHAR(64)  NOT NULL,
    lock_until  TIMESTAMP(3) NOT NULL,
    locked_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by   VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
