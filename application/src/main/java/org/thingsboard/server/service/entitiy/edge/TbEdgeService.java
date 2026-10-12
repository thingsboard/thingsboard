// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.edge;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;

import java.util.List;
import org.thingsboard.server.common.data.id.RuleChainId;

public interface TbEdgeService {

    Edge save(Edge edge, EntityGroup entityGroup, User user) throws Exception;

    Edge save(Edge edge, List<EntityGroup> entityGroups, User user) throws Exception;

    void delete(EdgeId edgeId, User user);

    void delete(Edge edge, User user);

    Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, User user) throws Exception;

}
