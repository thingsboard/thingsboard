package org.thingsboard.server.dao.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(prefix = "cassandra", value = "enabled", havingValue = "true")
public @interface NoSqlDao {
}
