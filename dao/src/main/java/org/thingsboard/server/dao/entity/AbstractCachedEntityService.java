// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.cache.TbTransactionalCache;

import java.io.Serializable;

public abstract class AbstractCachedEntityService<K extends Serializable, V extends Serializable, E> extends AbstractEntityService {

    @Autowired
    protected TbTransactionalCache<K, V> cache;

    protected void publishEvictEvent(E event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

    public abstract void handleEvictEvent(E event);

}
