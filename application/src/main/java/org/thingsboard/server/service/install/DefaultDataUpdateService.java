/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.List;
import java.util.UUID;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private InstallScripts installScripts;

    @Override
    public void updateData(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "1.4.0":
                log.info("Updating data from version 1.4.0 to 2.0.0 ...");
                tenantsDefaultRuleChainUpdater.updateEntities(null);
                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private PaginatedUpdater<String, Tenant> tenantsDefaultRuleChainUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected List<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink).getData();
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain ruleChain = ruleChainService.getRootTenantRuleChain(tenant.getId());
                        if (ruleChain == null) {
                            installScripts.createDefaultRuleChains(tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    public abstract class PaginatedUpdater<I, D extends IdBased<?>> {

        private static final int DEFAULT_LIMIT = 100;

        public void updateEntities(I id) {
            TextPageLink pageLink = new TextPageLink(DEFAULT_LIMIT);
            boolean hasNext = true;
            while (hasNext) {
                List<D> entities = findEntities(id, pageLink);
                for (D entity : entities) {
                    updateEntity(entity);
                }
                hasNext = entities.size() == pageLink.getLimit();
                if (hasNext) {
                    int index = entities.size() - 1;
                    UUID idOffset = entities.get(index).getUuidId();
                    pageLink.setIdOffset(idOffset);
                }
            }
        }

        protected abstract List<D> findEntities(I id, TextPageLink pageLink);

        protected abstract void updateEntity(D entity);

    }

}