// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.sync.enabled:true}' == 'true' && '${queue.type:null}' == 'in-memory'")
public class LocalEdqsSyncService extends EdqsSyncService {

    private final EdqsRocksDb db;

    @Override
    public boolean isSyncNeeded() {
        return db.isNew();
    }

}
