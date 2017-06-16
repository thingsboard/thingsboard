package org.thingsboard.server.dao.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true")
public @interface SqlDao {
}
