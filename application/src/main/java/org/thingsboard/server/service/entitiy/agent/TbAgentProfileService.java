// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;

import java.util.List;

public interface TbAgentProfileService {

    AgentProfile save(AgentProfile agentProfile, User currentUser) throws Exception;

    AgentProfile save(AgentProfile agentProfile, List<AgentAppProfileId> appProfileIds, User currentUser) throws Exception;

    void assignAppProfiles(AgentProfile agentProfile, List<AgentAppProfileId> appProfileIds, User currentUser) throws Exception;

    void setAppProfileRelatesOnAutoDiscovery(AgentProfile agentProfile, AgentAppProfileId appProfileId, boolean enable, User user) throws ThingsboardException;

    void delete(AgentProfile agentProfile, User user);

    AgentProfile setDefaultAgentProfile(AgentProfile agentProfile, AgentProfile previousDefaultAgentProfile, User user) throws ThingsboardException;
}
