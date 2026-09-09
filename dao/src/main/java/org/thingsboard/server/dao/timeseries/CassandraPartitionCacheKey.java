// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@AllArgsConstructor
public class CassandraPartitionCacheKey {

    private EntityId entityId;
    private String key;
    private long partition;

}