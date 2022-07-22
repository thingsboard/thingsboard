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
package org.thingsboard.server.common.data.event;

import lombok.Getter;

public enum EventType {
    ERROR("error_event"), LC_EVENT("lc_event"), STATS("stats_event"), DEBUG_RULE_NODE("rule_node_debug_event"), DEBUG_RULE_CHAIN("rule_chain_debug_event");

    @Getter
    private final String table;

    EventType(String table) {
        this.table = table;
    }

}