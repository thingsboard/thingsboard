// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Getter;
import lombok.ToString;

@ToString(exclude = "future")
public final class TbSqlQueueElement<E, R> {
    @Getter
    private final SettableFuture<R> future;
    @Getter
    private final E entity;

    public TbSqlQueueElement(SettableFuture<R> future, E entity) {
        this.future = future;
        this.entity = entity;
    }
}


