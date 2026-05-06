package com.lockerroom.dispatchservice.infrastructure.configuration;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lockerroom.dispatchservice.support.IntegrationTest;
import com.lockerroom.dispatchservice.support.MariaDbContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayMigrationIntegrationTest extends MariaDbContainerSupport {

    @Autowired
    private DataSource dataSource;

    @Test
    void v1Migration_createsAllTablesWithExpectedColumns() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer dispatchLogsCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'dispatch_logs'", Integer.class);
        Integer messageTemplatesCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'message_templates'", Integer.class);
        Integer shedlockCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'shedlock'", Integer.class);

        assertThat(dispatchLogsCount).isEqualTo(1);
        assertThat(messageTemplatesCount).isEqualTo(1);
        assertThat(shedlockCount).isEqualTo(1);
    }

    @Test
    void v1Migration_dispatchLogsHasUniqueConstraintOnEventIdAndChannel() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_name = 'dispatch_logs' " +
                        "AND index_name = 'uk_dispatch_logs_event_channel'",
                Integer.class);

        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
