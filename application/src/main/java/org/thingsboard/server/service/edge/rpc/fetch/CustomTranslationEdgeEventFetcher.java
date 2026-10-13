// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.translation.CustomTranslationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Slf4j
public class CustomTranslationEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomerService customerService;
    private final CustomTranslationService customTranslationService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        List<EdgeEvent> result = new ArrayList<>();
        EdgeId edgeId = edge.getId();

        processTenantLocales(TenantId.SYS_TENANT_ID, edgeId, result);
        processTenantLocales(tenantId, edgeId, result);

        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
            processCustomerLocales(tenantId, edge.getCustomerId(), edgeId, result);
        }

        return new PageData<>(result, 1, result.size(), false);
    }

    private void processTenantLocales(TenantId tenantId, EdgeId edgeId, List<EdgeEvent> result) {
        Set<String> locales = customTranslationService.getCurrentCustomizedLocales(tenantId, null);
        for (String locale : locales) {
            addLocaleEvent(tenantId, null, locale, edgeId, result);
        }
    }

    private void processCustomerLocales(TenantId tenantId, CustomerId customerId, EdgeId edgeId, List<EdgeEvent> result) {
        Set<String> customerLocales = customTranslationService.getCurrentCustomizedLocales(tenantId, customerId);
        for (String locale : customerLocales) {
            addLocaleEvent(tenantId, customerId, locale, edgeId, result);

            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer.isSubCustomer()) {
                getParentCustomerEvents(tenantId, customer.getParentCustomerId(), locale, edgeId, result);
            }
        }
    }

    private void getParentCustomerEvents(TenantId tenantId, CustomerId customerId, String locale, EdgeId edgeId, List<EdgeEvent> result) {
        addLocaleEvent(tenantId, customerId, locale, edgeId, result);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            getParentCustomerEvents(tenantId, customer.getParentCustomerId(), locale, edgeId, result);
        }
    }

    private void addLocaleEvent(TenantId tenantId, CustomerId customerId, String locale, EdgeId edgeId, List<EdgeEvent> events) {
        JsonNode value = customTranslationService.getCurrentCustomTranslation(tenantId, customerId, locale);
        CustomTranslation customTranslation = CustomTranslation.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .localeCode(locale)
                .value(value)
                .build();
        events.add(EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.CUSTOM_TRANSLATION, EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(customTranslation)));
    }

}
