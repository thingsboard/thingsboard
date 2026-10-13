// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresService {

    private final JdbcTemplate jdbcTemplate;

    public Blob getBlob(long oid) {
        byte[] data = jdbcTemplate.execute((ConnectionCallback<byte[]>) connection -> {
            connection.setAutoCommit(false);
            try {
                LargeObjectManager loManager = getLoManager(connection);
                try (LargeObject lo = loManager.open(oid, LargeObjectManager.READ)) {
                    return IOUtils.toByteArray(lo.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                connection.setAutoCommit(true);
            }
        });
        return new Blob(data);
    }

    public Long saveBlob(Blob blob) {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            LargeObjectManager largeObjectManager = getLoManager(connection);
            long oid = largeObjectManager.createLO();
            try (LargeObject lo = largeObjectManager.open(oid, LargeObjectManager.WRITE)) {
                lo.write(blob.data());
            }
            log.info("Created blob {} (data size: {})", oid, blob.data().length);
            return oid;
        });
    }

    private LargeObjectManager getLoManager(Connection connection) throws SQLException {
        return connection.unwrap(PgConnection.class).getLargeObjectAPI();
    }

    public record Blob(byte[] data) {}

}
