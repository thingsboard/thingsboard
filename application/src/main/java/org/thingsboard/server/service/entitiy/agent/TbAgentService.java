// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.group.EntityGroup;

import java.util.List;

public interface TbAgentService {
    Agent save(Agent agent, User currentUser) throws Exception;
    Agent save(Agent agent, List<EntityGroup> entityGroups, User currentUser) throws Exception;
    void delete(Agent agent, User user);
}
