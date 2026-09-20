package com.cozy.user.mq;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserEventOutboxMigrationContractTest {

    @Test
    void schemaExpressesTagScopedUniquenessAndFixedTopic() throws Exception {
        try (var stream = getClass().getResourceAsStream("/db/migration/V3__add_user_event_outbox.sql")) {
            assertNotNull(stream);
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase().replace("`", "").replaceAll("\\s+", " ");

            assertTrue(sql.contains("unique key uk_tag_unique_key (tag, unique_key)"));
            assertTrue(sql.contains("tag varchar(64) not null"));
            assertFalse(sql.contains(" topic "), "topic 固定为 USER_EVENTS，不应形成第二个数据源");
        }
    }
}
