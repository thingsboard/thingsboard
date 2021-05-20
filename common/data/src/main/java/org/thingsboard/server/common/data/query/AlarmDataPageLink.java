/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;

import java.util.List;

@Data
@AllArgsConstructor
public class AlarmDataPageLink extends EntityDataPageLink {

    private long startTs;
    private long endTs;
    //TODO: handle this;
    private long timeWindow;
    private List<String> typeList;
    private List<AlarmSearchStatus> statusList;
    private List<AlarmSeverity> severityList;
    private boolean searchPropagatedAlarms;

    public AlarmDataPageLink() {
        super();
    }

    public AlarmDataPageLink(int pageSize, int page, String textSearch, EntityDataSortOrder sortOrder, boolean dynamic,
                             boolean searchPropagatedAlarms,
                             long startTs, long endTs, long timeWindow,
                             List<String> typeList, List<AlarmSearchStatus> statusList, List<AlarmSeverity> severityList) {
        super(pageSize, page, textSearch, sortOrder, dynamic);
        this.searchPropagatedAlarms = searchPropagatedAlarms;
        this.startTs = startTs;
        this.endTs = endTs;
        this.timeWindow = timeWindow;
        this.typeList = typeList;
        this.statusList = statusList;
        this.severityList = severityList;
    }

    @JsonIgnore
    public AlarmDataPageLink nextPageLink() {
        return new AlarmDataPageLink(this.getPageSize(), this.getPage() + 1, this.getTextSearch(), this.getSortOrder(), this.isDynamic(),
                this.searchPropagatedAlarms,
                this.startTs, this.endTs, this.timeWindow,
                this.typeList, this.statusList, this.severityList
        );
    }
}
