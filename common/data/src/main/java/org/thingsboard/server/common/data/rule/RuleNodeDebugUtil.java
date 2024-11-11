/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.rule;

import org.thingsboard.server.common.data.msg.TbNodeConnectionType;

import java.util.Set;

public final class RuleNodeDebugUtil {
    private RuleNodeDebugUtil() {}

    public static boolean isDebugAllAvailable(RuleNode ruleNode) {
        return ruleNode.getDebugAllUntil() > System.currentTimeMillis();
    }

    public static boolean isDebugAvailable(RuleNode ruleNode, String nodeConnection) {
        return isDebugAllAvailable(ruleNode) || ruleNode.isDebugFailures() && TbNodeConnectionType.FAILURE.equals(nodeConnection);
    }

    public static boolean isDebugFailuresAvailable(RuleNode ruleNode, Set<String> nodeConnections) {
        return isDebugFailuresAvailable(ruleNode) && nodeConnections.contains(TbNodeConnectionType.FAILURE);
    }

    public static boolean isDebugFailuresAvailable(RuleNode ruleNode, String nodeConnection) {
        return isDebugFailuresAvailable(ruleNode) && TbNodeConnectionType.FAILURE.equals(nodeConnection);
    }

    public static boolean isDebugFailuresAvailable(RuleNode ruleNode) {
        return ruleNode.isDebugFailures() || isDebugAllAvailable(ruleNode);
    }

}
