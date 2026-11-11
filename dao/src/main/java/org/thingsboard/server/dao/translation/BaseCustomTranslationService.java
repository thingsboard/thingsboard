// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.translation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;

import java.util.List;
import java.util.Set;

import static org.thingsboard.common.util.JacksonUtil.deleteByKeyPath;
import static org.thingsboard.common.util.JacksonUtil.merge;
import static org.thingsboard.common.util.JacksonUtil.update;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseCustomTranslationService extends AbstractCachedService<CustomTranslationCompositeKey, CustomTranslation, CustomTranslationEvictEvent> implements CustomTranslationService {

    private final CustomerService customerService;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomTranslationDao customTranslationDao;

    @Override
    public JsonNode getCurrentCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        CustomTranslationCompositeKey key = new CustomTranslationCompositeKey(tenantId, customerId, localeCode);
        CustomTranslation customTranslation = cache.getAndPutInTransaction(key, () -> customTranslationDao.findById(TenantId.SYS_TENANT_ID, key), true);
        if (customTranslation == null) {
            customTranslation = CustomTranslation.builder()
                    .localeCode(key.getLocaleCode())
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .value(JacksonUtil.newObjectNode()).build();
        }
        return customTranslation.getValue().deepCopy();
    }

    @Override
    public JsonNode getMergedTenantCustomTranslation(TenantId tenantId, String localeCode) {
        JsonNode tenantCustomTranslation = getCurrentCustomTranslation(tenantId, null, localeCode);
        JsonNode systemCustomTranslation = getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode);
        return merge(systemCustomTranslation, tenantCustomTranslation);
    }

    @Override
    public JsonNode getMergedCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customerCustomTranslation = getCurrentCustomTranslation(tenantId, customerId, localeCode);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            customerCustomTranslation = getMergedCustomerHierarchyCustomTranslation(tenantId, customer.getParentCustomerId(), localeCode, customerCustomTranslation);
        }
        JsonNode tenantCustomTranslation = getCurrentCustomTranslation(tenantId, null, localeCode);
        JsonNode systemCustomTranslation = getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode);

        return merge(systemCustomTranslation, merge(tenantCustomTranslation, customerCustomTranslation));
    }

    @Override
    public void saveCustomTranslation(CustomTranslation customTranslation) {
        customTranslationDao.save(customTranslation.getTenantId(), customTranslation);
        publishEvictEvent(new CustomTranslationEvictEvent(new CustomTranslationCompositeKey(customTranslation.getTenantId(), customTranslation.getCustomerId(), customTranslation.getLocaleCode())));
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(customTranslation.getTenantId())
                .entityId(getEntityIdForEvent(customTranslation.getTenantId(), customTranslation.getCustomerId()))
                .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION)
                .actionType(ActionType.UPDATED)
                .body(JacksonUtil.toString(customTranslation)).build());
    }

    @Override
    public void patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode updateValue) {
        JsonNode currentCustomTranslation = getCurrentCustomTranslation(tenantId, customerId, localeCode);
        CustomTranslation newCustomTranslation = CustomTranslation.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .localeCode(localeCode)
                .value(update(currentCustomTranslation, updateValue))
                .build();
        saveCustomTranslation(newCustomTranslation);
    }

    @Override
    public void deleteCustomTranslationKeyByPath(TenantId tenantId, CustomerId customerId, String localeCode, String keyPath) {
        JsonNode currentCustomTranslation = getCurrentCustomTranslation(tenantId, customerId, localeCode);
        CustomTranslation newCustomTranslation = CustomTranslation.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .localeCode(localeCode)
                .value(deleteByKeyPath(currentCustomTranslation, keyPath))
                .build();
        saveCustomTranslation(newCustomTranslation);
    }

    @Override
    public void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        deleteCustomTranslation(tenantId, customerId, localeCode, true);
    }

    private void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, boolean publishEvent) {
        JsonNode value = getCurrentCustomTranslation(tenantId, customerId, localeCode);
        CustomTranslationCompositeKey key = new CustomTranslationCompositeKey(tenantId, customerId, localeCode);
        customTranslationDao.removeById(tenantId, key);
        publishEvictEvent(new CustomTranslationEvictEvent(new CustomTranslationCompositeKey(tenantId, customerId, localeCode)));
        if (publishEvent) {
            CustomTranslation customTranslation = CustomTranslation.builder().tenantId(tenantId).customerId(customerId).localeCode(localeCode).value(value).build();
            eventPublisher.publishEvent(ActionEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(getEntityIdForEvent(tenantId, customerId))
                    .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION)
                    .actionType(ActionType.DELETED)
                    .body(JacksonUtil.toString(customTranslation)).build());
        }
    }

    @Override
    public Set<String> getCurrentCustomizedLocales(TenantId tenantId, CustomerId customerId) {
        return customTranslationDao.findLocalesByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Override
    public Set<String> getMergedTenantCustomizedLocales(TenantId tenantId) {
        Set<String> customizedLocales = customTranslationDao.findLocalesByTenantIdAndCustomerId(tenantId, null);
        customizedLocales.addAll(customTranslationDao.findLocalesByTenantIdAndCustomerId(TenantId.SYS_TENANT_ID, null));
        return customizedLocales;
    }

    @Override
    public Set<String> getMergedCustomerCustomizedLocales(TenantId tenantId, CustomerId customerId) {
        Set<String> customizedLocales = customTranslationDao.findLocalesByTenantIdAndCustomerId(tenantId, customerId);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            customizedLocales.addAll(getMergedCustomerHierarchyLocales(tenantId, customer.getParentCustomerId()));
        }
        customizedLocales.addAll(customTranslationDao.findLocalesByTenantIdAndCustomerId(tenantId, null));
        customizedLocales.addAll(customTranslationDao.findLocalesByTenantIdAndCustomerId(TenantId.SYS_TENANT_ID, null));
        return customizedLocales;
    }

    @Override
    public void deleteCustomTranslationByTenantId(TenantId tenantId) {
        List<CustomTranslationCompositeKey> customTranslationIds = customTranslationDao.findCustomTranslationByTenantId(tenantId.getId());
        for (CustomTranslationCompositeKey customTranslationId : customTranslationIds) {
            deleteCustomTranslation(TenantId.fromUUID(customTranslationId.getTenantId()), new CustomerId(customTranslationId.getCustomerId()), customTranslationId.getLocaleCode(), false);
        }
    }

    private JsonNode getMergedCustomerHierarchyCustomTranslation(TenantId tenantId, CustomerId customerId, String locale, JsonNode customTranslation) {
        JsonNode parentCustomerCustomTranslation = getCurrentCustomTranslation(tenantId, customerId, locale);
        JsonNode merged = merge(parentCustomerCustomTranslation, customTranslation);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            return getMergedCustomerHierarchyCustomTranslation(tenantId, customer.getParentCustomerId(), locale, merged);
        } else {
            return merged;
        }
    }

    private Set<String> getMergedCustomerHierarchyLocales(TenantId tenantId, CustomerId customerId) {
        Set<String> parentCustomizedLocales = customTranslationDao.findLocalesByTenantIdAndCustomerId(tenantId, customerId);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            parentCustomizedLocales.addAll(getMergedCustomerHierarchyLocales(tenantId, customer.getParentCustomerId()));
        }
        return parentCustomizedLocales;
    }

    private static EntityId getEntityIdForEvent(TenantId tenantId, CustomerId customerId) {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @TransactionalEventListener(classes = CustomTranslationEvictEvent.class)
    @Override
    public void handleEvictEvent(CustomTranslationEvictEvent event) {
        cache.evict(event.getKey());
    }

}
