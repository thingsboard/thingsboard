// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.wl;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.resource.ImageCacheKey;

public interface WhiteLabelingService {

    WhiteLabelingParams getSystemWhiteLabelingParams();

    LoginWhiteLabelingParams getSystemLoginWhiteLabelingParams();

    WhiteLabelingParams getTenantWhiteLabelingParams(TenantId tenantId);

    WhiteLabelingParams getCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId);

    WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId tenantId) throws Exception;

    WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId) throws Exception;

    LoginWhiteLabelingParams getTenantLoginWhiteLabelingParams(TenantId tenantId) throws Exception;

    LoginWhiteLabelingParams getCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId) throws Exception;

    LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(String domainName) throws Exception;

    ImageCacheKey getLoginImageKey(String domainName, boolean faviconElseLogo) throws Exception;

    WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams);

    WhiteLabelingParams saveTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams);

    WhiteLabelingParams saveCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams);

    LoginWhiteLabelingParams saveSystemLoginWhiteLabelingParams(LoginWhiteLabelingParams loginWhiteLabelingParams);

    LoginWhiteLabelingParams saveTenantLoginWhiteLabelingParams(TenantId tenantId, LoginWhiteLabelingParams loginWhiteLabelingParams) throws Exception;

    LoginWhiteLabelingParams saveCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelingParams) throws Exception;

    WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams);

    WhiteLabelingParams mergeTenantWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams);

    WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams);

    void deleteAllTenantWhiteLabeling(TenantId tenantId);

    boolean isWhiteLabelingAllowed(TenantId tenantId, CustomerId customerId);

    boolean isCustomerWhiteLabelingAllowed(TenantId tenantId);

    boolean isWhiteLabelingConfigured(TenantId tenantId);

    JsonNode saveMailTemplates(TenantId tenantId, JsonNode mailTemplates);

    JsonNode getCurrentTenantMailTemplates(TenantId tenantId, boolean systemByDefault);

    JsonNode findMailTemplatesByTenantId(TenantId tenantId, TenantId settingsTenantId);

    JsonNode getMergedTenantMailTemplates(TenantId tenantId) throws ThingsboardException;

    WhiteLabeling findByEntityId(TenantId tenantId, CustomerId customerId, WhiteLabelingType type);

    WhiteLabeling findWhiteLabelingByDomainAndType(String domainName, WhiteLabelingType type);

    WebSelfRegistrationParams saveTenantSelfRegistrationParams(TenantId tenantId, WebSelfRegistrationParams selfRegistrationParams);

    WebSelfRegistrationParams getTenantSelfRegistrationParams(TenantId tenantId);

    WebSelfRegistrationParams getWebSelfRegistrationParams(String domainName);

    JsonNode getWebPrivacyPolicy(String domainName);

    JsonNode getTenantPrivacyPolicy(TenantId tenantId);

    JsonNode getWebTermsOfUse(String domainName);

    JsonNode getTenantTermsOfUse(TenantId tenantId);

    void deleteWhiteLabeling(TenantId tenantId, CustomerId customerId, WhiteLabelingType type);

}
