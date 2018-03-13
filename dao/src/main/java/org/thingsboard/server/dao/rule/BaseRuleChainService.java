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

package org.thingsboard.server.dao.rule;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;

/**
 * Created by igor on 3/12/18.
 */
@Service
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

    public static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);

    @Autowired
    private RuleChainDao ruleChainDao;

    @Override
    public RuleChain saveRuleChain(RuleChain ruleChain) {
        ruleChainValidator.validate(ruleChain);
        if (ruleChain.getTenantId() == null) {
            log.trace("Save system rule chain with predefined id {}", SYSTEM_TENANT);
            ruleChain.setTenantId(SYSTEM_TENANT);
        }
        return ruleChainDao.save(ruleChain);
    }

    @Override
    public RuleChain findRuleChainById(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findById(ruleChainId.getId());
    }

    @Override
    public TextPageData<RuleChain> findSystemRuleChains(TextPageLink pageLink) {
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search system rule chain request.");
        List<RuleChain> ruleChains = ruleChainDao.findRuleChainsByTenantId(SYSTEM_TENANT.getId(), pageLink);
        return new TextPageData<>(ruleChains, pageLink);
    }

    @Override
    public TextPageData<RuleChain> findTenantRuleChains(TenantId tenantId, TextPageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search rule chain request.");
        List<RuleChain> ruleChains = ruleChainDao.findRuleChainsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(ruleChains, pageLink);
    }

    @Override
    public TextPageData<RuleChain> findAllTenantRuleChainsByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findAllTenantRuleChainsByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<RuleChain> ruleChains = ruleChainDao.findAllRuleChainsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(ruleChains, pageLink);
    }

    @Override
    public void deleteRuleChainById(RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        checkRuleNodesAndDelete(ruleChainId);
    }

    private void checkRuleNodesAndDelete(RuleChainId ruleChainId) {
        //TODO:
        deleteEntityRelations(ruleChainId);
        ruleChainDao.removeById(ruleChainId.getId());
    }

    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId);
    }

    private DataValidator<RuleChain> ruleChainValidator =
            new DataValidator<RuleChain>() {
                @Override
                protected void validateDataImpl(RuleChain ruleChain) {
                    if (StringUtils.isEmpty(ruleChain.getName())) {
                        throw new DataValidationException("Rule chain name should be specified!.");
                    }
                }
            };

    private PaginatedRemover<TenantId, RuleChain> tenantRuleChainsRemover =
            new PaginatedRemover<TenantId, RuleChain>() {

                @Override
                protected List<RuleChain> findEntities(TenantId id, TextPageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(RuleChain entity) {
                    checkRuleNodesAndDelete(entity.getId());
                }
            };
}
