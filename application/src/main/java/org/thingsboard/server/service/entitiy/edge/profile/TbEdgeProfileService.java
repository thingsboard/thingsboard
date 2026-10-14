// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.edge.profile;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;

public interface TbEdgeProfileService {

    EdgeProfile save(EdgeProfile edgeProfile, User user) throws Exception;

    void delete(EdgeProfile edgeProfile, User user);

    EdgeProfile setDefaultEdgeProfile(EdgeProfile edgeProfile, EdgeProfile previousDefaultEdgeProfile, User user);

    EdgeProfile addRuleChain(EdgeProfile edgeProfile, RuleChainId ruleChainId, User user);

    EdgeProfile removeRuleChain(EdgeProfile edgeProfile, RuleChainId ruleChainId, User user);

    EdgeProfile setRootRuleChain(EdgeProfile edgeProfile, RuleChainId ruleChainId, User user);

    EdgeProfile addEntityGroup(EdgeProfile edgeProfile, EntityGroupId entityGroupId, EntityType groupType, User user);

    EdgeProfile removeEntityGroup(EdgeProfile edgeProfile, EntityGroupId entityGroupId, User user);

    EdgeProfile addIntegration(EdgeProfile edgeProfile, IntegrationId integrationId, User user);

    EdgeProfile removeIntegration(EdgeProfile edgeProfile, IntegrationId integrationId, User user);

    EdgeProfile addSchedulerEvent(EdgeProfile edgeProfile, SchedulerEventId schedulerEventId, User user);

    EdgeProfile removeSchedulerEvent(EdgeProfile edgeProfile, SchedulerEventId schedulerEventId, User user);

    void reapply(EdgeProfile edgeProfile, User user);

}
