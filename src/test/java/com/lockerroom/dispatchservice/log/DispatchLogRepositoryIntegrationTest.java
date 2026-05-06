package com.lockerroom.dispatchservice.log;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.lockerroom.dispatchservice.infrastructure.configuration.JpaConfig;
import com.lockerroom.dispatchservice.support.IntegrationTest;
import com.lockerroom.dispatchservice.support.MariaDbContainerSupport;

import net.javacrumbs.shedlock.core.LockProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class DispatchLogRepositoryIntegrationTest extends MariaDbContainerSupport {

    @MockitoBean
    private LockProvider lockProvider;

    @Autowired
    private DispatchLogRepository repository;

    @Test
    void save_setsCreatedAtAndUpdatedAt() {
        DispatchLog saved = repository.saveAndFlush(newLog("evt-1", DispatchChannel.MAIL));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void duplicateEventIdAndChannel_throwsDataIntegrityViolation() {
        repository.saveAndFlush(newLog("evt-dup", DispatchChannel.MAIL));

        assertThatThrownBy(() -> repository.saveAndFlush(newLog("evt-dup", DispatchChannel.MAIL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameEventIdDifferentChannel_isAllowed() {
        repository.saveAndFlush(newLog("evt-multi", DispatchChannel.MAIL));
        repository.saveAndFlush(newLog("evt-multi", DispatchChannel.SMS));

        assertThat(repository.existsByEventIdAndChannel("evt-multi", DispatchChannel.MAIL)).isTrue();
        assertThat(repository.existsByEventIdAndChannel("evt-multi", DispatchChannel.SMS)).isTrue();
    }

    private DispatchLog newLog(String eventId, DispatchChannel channel) {
        return DispatchLog.builder()
                .eventId(eventId)
                .eventType(DispatchEventType.NOTI_COMMENT)
                .channel(channel)
                .status(DispatchStatus.PENDING)
                .retryCount(0)
                .targetUserId(1L)
                .build();
    }
}
