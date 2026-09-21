// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.component;

import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
public interface ComponentDiscoveryService {

    void discoverComponents();

    Optional<RuleNodeClassInfo> getRuleNodeInfo(String clazz);

    List<RuleNodeClassInfo> getVersionedNodes();

    List<ComponentDescriptor> getComponents(ComponentType type, RuleChainType ruleChainType);

    List<ComponentDescriptor> getComponents(Set<ComponentType> types, RuleChainType ruleChainType);

    Optional<ComponentDescriptor> getComponent(String clazz);
}
