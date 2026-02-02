/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.service.ws.WsCmdType;

public class EntityDataCmd extends DataCmd {

    @Getter
    private final EntityDataQuery query;
    @Getter
    private final EntityHistoryCmd historyCmd;
    @Getter
    private final LatestValueCmd latestCmd;
    @Getter
    private final TimeSeriesCmd tsCmd;
    @Getter
    private final AggHistoryCmd aggHistoryCmd;
    @Getter
    private final AggTimeSeriesCmd aggTsCmd;

    public EntityDataCmd(int cmdId, EntityDataQuery query, EntityHistoryCmd historyCmd, LatestValueCmd latestCmd, TimeSeriesCmd tsCmd) {
        this(cmdId, query, historyCmd, latestCmd, tsCmd, null, null);
    }

    @JsonCreator
    public EntityDataCmd(@JsonProperty("cmdId") int cmdId,
                         @JsonProperty("query") EntityDataQuery query,
                         @JsonProperty("historyCmd") EntityHistoryCmd historyCmd,
                         @JsonProperty("latestCmd") LatestValueCmd latestCmd,
                         @JsonProperty("tsCmd") TimeSeriesCmd tsCmd,
                         @JsonProperty("aggHistoryCmd") AggHistoryCmd aggHistoryCmd,
                         @JsonProperty("aggTsCmd") AggTimeSeriesCmd aggTsCmd) {
        super(cmdId);
        this.query = query;
        this.historyCmd = historyCmd;
        this.latestCmd = latestCmd;
        this.tsCmd = tsCmd;
        this.aggHistoryCmd = aggHistoryCmd;
        this.aggTsCmd = aggTsCmd;
    }

    @JsonIgnore
    public boolean hasAnyCmd() {
        return historyCmd != null || latestCmd != null || tsCmd != null || aggHistoryCmd != null || aggTsCmd != null;
    }

    @Override
    public WsCmdType getType() {
        return WsCmdType.ENTITY_DATA;
    }
}
