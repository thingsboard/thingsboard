// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CitusSettings {

    @Value("${database.citus.enabled:false}")
    private boolean enabled;

    @Value("${database.citus.shard_count:32}")
    private int shardCount;

}
