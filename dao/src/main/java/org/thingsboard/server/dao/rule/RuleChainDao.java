/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.rule;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.ResourceContainerDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by igor on 3/12/18.
 */
public interface RuleChainDao extends Dao<RuleChain>, TenantEntityDao<RuleChain>, ExportableEntityDao<RuleChainId, RuleChain>, ResourceContainerDao<RuleChain> {

    /**
     * Find rule chains by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    PageData<RuleChain> findRuleChainsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find rule chains by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    PageData<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, PageLink pageLink);

    /**
     * Find root rule chain by tenantId and type
     *
     * @param tenantId the tenantId
     * @param type the type
     * @return the rule chain object
     */
    RuleChain findRootRuleChainByTenantIdAndType(UUID tenantId, RuleChainType type);

    /**
     * Find rule chains by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    /**
     * Find auto assign to edge rule chains by tenantId.
     *
     * @param tenantId the tenantId
     * @return the list of rule chain objects
     */
    PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(UUID tenantId, PageLink pageLink);

    Collection<RuleChain> findByTenantIdAndTypeAndName(TenantId tenantId, RuleChainType type, String name);

}
