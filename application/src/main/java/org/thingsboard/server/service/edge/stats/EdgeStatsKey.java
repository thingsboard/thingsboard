/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.stats;

public enum EdgeStatsKey {
    DOWNLINK_MSG_PUSHED,
    DOWNLINK_MSG_PERMANENTLY_FAILED,
    DOWNLINK_MSG_TMP_FAILED,
    DOWNLINK_MSG_ADDED;

    public static final String DOWNLINK_MSGS_ADDED = "downlinkMsgsAdded";
    public static final String DOWNLINK_MSGS_PUSHED = "downlinkMsgsPushed";
    public static final String DOWNLINK_MSGS_PERMANENTLY_FAILED = "downlinkMsgsPermanentlyFailed";
    public static final String DOWNLINK_MSGS_TMP_FAILED = "downlinkMsgsTmpFailed";
    public static final String DOWNLINK_MSGS_LAG = "downlinkMsgsLag";

}
