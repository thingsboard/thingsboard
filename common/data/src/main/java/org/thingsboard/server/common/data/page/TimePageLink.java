/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

@ToString
public class TimePageLink extends BasePageLink implements Serializable {

    private static final long serialVersionUID = -4189954843653250480L;

    @Getter private final Long startTime;
    @Getter private final Long endTime;
    @Getter private final boolean ascOrder;

    public TimePageLink(int limit) {
        this(limit, null, null, false, null);
    }

    public TimePageLink(int limit, Long startTime) {
        this(limit, startTime, null, false, null);
    }

    public TimePageLink(int limit, Long startTime, Long endTime) {
        this(limit, startTime, endTime, false, null);
    }

    public TimePageLink(int limit, Long startTime, Long endTime, boolean ascOrder) {
        this(limit, startTime, endTime, ascOrder, null);
    }

    @JsonCreator
    public TimePageLink(@JsonProperty("limit") int limit,
                        @JsonProperty("startTime") Long startTime,
                        @JsonProperty("endTime") Long endTime,
                        @JsonProperty("ascOrder") boolean ascOrder,
                        @JsonProperty("idOffset") UUID idOffset) {
        super(limit, idOffset);
        this.startTime = startTime;
        this.endTime = endTime;
        this.ascOrder = ascOrder;
    }
}
