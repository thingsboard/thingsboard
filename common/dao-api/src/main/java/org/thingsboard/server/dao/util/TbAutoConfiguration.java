// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration",
        "org.springframework.boot.data.cassandra.autoconfigure.DataCassandraAutoConfiguration",
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
})
public @interface TbAutoConfiguration {
}
