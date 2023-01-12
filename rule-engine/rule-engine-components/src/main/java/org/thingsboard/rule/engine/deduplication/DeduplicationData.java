/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.deduplication;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeduplicationData {

    private EntityId entityId;
    private List<DeduplicationPack> deduplicationPacks;
    private int statesCount;

    public DeduplicationData(EntityId entityId) {
        this.entityId = entityId;
        this.deduplicationPacks = new ArrayList<>();
        this.statesCount = 0;
    }

    public void addDeduplicationPack(List<TbMsgDeduplicationState> states, int indexOfFirstStateInPack, int indexOfLastStateInPack) {
        deduplicationPacks.add(new DeduplicationPack(indexOfFirstStateInPack, indexOfLastStateInPack, states));
        statesCount += states.size();
    }

}
