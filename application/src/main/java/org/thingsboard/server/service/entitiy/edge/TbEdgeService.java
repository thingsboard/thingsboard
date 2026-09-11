// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.edge;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;

public interface TbEdgeService {

    Edge save(Edge edge, RuleChain edgeTemplateRootRuleChain, User user) throws Exception;

    void delete(Edge edge, User user);

    Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, Customer customer, User user) throws ThingsboardException;

    Edge unassignEdgeFromCustomer(Edge edge, Customer customer, User user) throws ThingsboardException;

    Edge assignEdgeToPublicCustomer(TenantId tenantId, EdgeId edgeId, User user) throws ThingsboardException;

    Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, User user) throws Exception;

}
