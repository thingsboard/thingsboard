// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.wl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.net.URI;

import static org.thingsboard.server.common.data.wl.WhiteLabelingType.LOGIN;
import static org.thingsboard.server.common.data.wl.WhiteLabelingType.PRIVACY_POLICY;
import static org.thingsboard.server.common.data.wl.WhiteLabelingType.SELF_REGISTRATION;
import static org.thingsboard.server.common.data.wl.WhiteLabelingType.TERMS_OF_USE;
import static org.thingsboard.server.dao.entity.AbstractEntityService.checkConstraintViolation;
import static org.thingsboard.server.dao.service.DataValidator.isValidUrl;
import static org.thingsboard.server.dao.wl.WhiteLabelingCacheKey.forKey;
import static org.thingsboard.server.dao.wl.WhiteLabelingCacheKey.forTypeAndDomain;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseWhiteLabelingService extends AbstractCachedService<WhiteLabelingCacheKey, WhiteLabeling, WhiteLabelingEvictEvent> implements WhiteLabelingService {

    private static final String ALLOW_WHITE_LABELING = "allowWhiteLabeling";
    private static final String ALLOW_CUSTOMER_WHITE_LABELING = "allowCustomerWhiteLabeling";

    private final AdminSettingsService adminSettingsService;
    private final DomainService domainService;
    private final WhiteLabelingDao whiteLabelingDao;
    private final TenantService tenantService;
    private final CustomerService customerService;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LoginWhiteLabelingParams getSystemLoginWhiteLabelingParams() {
        WhiteLabeling whiteLabeling = findByEntityId(TenantId.SYS_TENANT_ID, null, LOGIN);
        return constructLoginWlParams(whiteLabeling);
    }

    @Override
    public WhiteLabelingParams getSystemWhiteLabelingParams() {
        WhiteLabeling whiteLabeling = findByEntityId(TenantId.SYS_TENANT_ID, null, WhiteLabelingType.GENERAL);
        return constructWlParams(whiteLabeling != null ? whiteLabeling.getSettings() : null, true);
    }

    @Override
    public WhiteLabelingParams getTenantWhiteLabelingParams(TenantId tenantId) {
        return getEntityWhiteLabelParams(tenantId, null);
    }

    @Override
    public WhiteLabelingParams getCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        return getEntityWhiteLabelParams(tenantId, customerId);
    }

    @Override
    public LoginWhiteLabelingParams getTenantLoginWhiteLabelingParams(TenantId tenantId) {
        return getEntityLoginWhiteLabelParams(tenantId, null);
    }

    @Override
    public LoginWhiteLabelingParams getCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        return getEntityLoginWhiteLabelParams(tenantId, customerId);
    }

    @Override
    public LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(String domainName) throws Exception {
        LoginWhiteLabelingParams result;
        WhiteLabeling existingLoginWLSettings;
        if (isUsedOnSystemLevel(domainName) && ((existingLoginWLSettings = whiteLabelingDao.findByDomainAndType(TenantId.SYS_TENANT_ID, domainName, LOGIN)) != null)) {
            var tenantId = existingLoginWLSettings.getTenantId();
            var customerId = existingLoginWLSettings.getCustomerId();
            result = getEntityLoginWhiteLabelParams(tenantId, customerId);
            if (customerId != null && !customerId.isNullUid()) {
                Customer customer = customerService.findCustomerById(tenantId, customerId);
                if (customer.isSubCustomer()) {
                    result = getCustomerHierarchyLoginWhileLabelingParams(tenantId, customer.getParentCustomerId(), result);
                }
                result.merge(getTenantLoginWhiteLabelingParams(tenantId));
            }
            result.merge(getSystemLoginWhiteLabelingParams());
        } else {
            result = getSystemLoginWhiteLabelingParams();
        }
        return result;
    }

    @Override
    public ImageCacheKey getLoginImageKey(String domainName, boolean faviconElseLogo) throws Exception {
        // TODO: optimize via cache
        LoginWhiteLabelingParams result;
        WhiteLabeling existingLoginWLSettings;
        TenantId tenantId = null;
        if (isUsedOnSystemLevel(domainName) && ((existingLoginWLSettings = whiteLabelingDao.findByDomainAndType(TenantId.SYS_TENANT_ID, domainName, LOGIN)) != null)) {
            tenantId = existingLoginWLSettings.getTenantId();
            var customerId = existingLoginWLSettings.getCustomerId();
            result = getEntityLoginWhiteLabelParams(tenantId, customerId);
            if (customerId != null && !customerId.isNullUid()) {
                Customer customer = customerService.findCustomerById(tenantId, customerId);
                if (customer.isSubCustomer()) {
                    result = getCustomerHierarchyLoginWhileLabelingParams(tenantId, customer.getParentCustomerId(), result);
                }
                result.merge(getTenantLoginWhiteLabelingParams(tenantId));
            }
            result.merge(getSystemLoginWhiteLabelingParams());
        } else {
            result = getSystemLoginWhiteLabelingParams();
        }
        var url = faviconElseLogo ? (result.getFavicon() != null ? result.getFavicon().getUrl() : null) : result.getLogoImageUrl();
        if (StringUtils.isNotBlank(url)) {
            TenantId imageTenantId = null;
            if (url.startsWith(DataConstants.TB_IMAGE_PREFIX + "/api/images/tenant/")) {
                imageTenantId = tenantId;
            } else if (url.startsWith(DataConstants.TB_IMAGE_PREFIX + "/api/images/system/")) {
                imageTenantId = TenantId.SYS_TENANT_ID;
            }
            if (imageTenantId != null) {
                var parts = url.split("/");
                if (parts.length == 5) {
                    var key = parts[4];
                    return ImageCacheKey.forImage(imageTenantId, key);
                }
            }
        }
        return null;
    }

    private LoginWhiteLabelingParams getCustomerHierarchyLoginWhileLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams childCustomerWLLParams) throws Exception {
        LoginWhiteLabelingParams entityLoginWhiteLabelParams = getEntityLoginWhiteLabelParams(tenantId, customerId);
        childCustomerWLLParams.merge(entityLoginWhiteLabelParams);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            return getCustomerHierarchyLoginWhileLabelingParams(tenantId, customer.getParentCustomerId(), childCustomerWLLParams);
        } else {
            return childCustomerWLLParams;
        }
    }

    @Override
    public WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId tenantId) {
        WhiteLabelingParams result = getTenantWhiteLabelingParams(tenantId);
        return mergeTenantWhiteLabelingParams(result);
    }

    @Override
    public WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        WhiteLabelingParams result = getCustomerWhiteLabelingParams(tenantId, customerId);
        return mergeCustomerWhiteLabelingParams(tenantId, customerId, result);
    }

    private WhiteLabelingParams getMergedCustomerHierarchyWhileLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams childCustomerWLParams) {
        WhiteLabelingParams entityWhiteLabelParams = getEntityWhiteLabelParams(tenantId, customerId);
        childCustomerWLParams.merge(entityWhiteLabelParams);
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            return getMergedCustomerHierarchyWhileLabelingParams(tenantId, customer.getParentCustomerId(), childCustomerWLParams);
        } else {
            return childCustomerWLParams;
        }
    }

    @Override
    public WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        saveWhiteLabelParams(TenantId.SYS_TENANT_ID, null, whiteLabelingParams);
        return getSystemWhiteLabelingParams();
    }

    @Override
    public LoginWhiteLabelingParams saveSystemLoginWhiteLabelingParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        if (loginWhiteLabelingParams.getDomainId() != null) {
            throw new DataValidationException("Domain is prohibited for system level");
        }
        if (loginWhiteLabelingParams.getBaseUrl() != null && !isValidUrl(loginWhiteLabelingParams.getBaseUrl())) {
            throw new IncorrectParameterException("Base url " + loginWhiteLabelingParams.getBaseUrl() + " is invalid");
        }
        saveLoginWhiteLabelParams(TenantId.SYS_TENANT_ID, null, loginWhiteLabelingParams);
        return getSystemLoginWhiteLabelingParams();
    }

    @Override
    public LoginWhiteLabelingParams saveTenantLoginWhiteLabelingParams(TenantId tenantId, LoginWhiteLabelingParams loginWhiteLabelingParams) {
        saveEntityLoginWhiteLabelingParams(tenantId, null, loginWhiteLabelingParams);
        return getTenantLoginWhiteLabelingParams(tenantId);
    }

    @Override
    public LoginWhiteLabelingParams saveCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelingParams) {
        saveEntityLoginWhiteLabelingParams(tenantId, customerId, loginWhiteLabelingParams);
        return getCustomerLoginWhiteLabelingParams(tenantId, customerId);
    }

    private boolean isUsedOnSystemLevel(String domainName) {
        try {
            LoginWhiteLabelingParams systemParams = getSystemLoginWhiteLabelingParams();
            if (systemParams != null) {
                if (isBaseUrlMatchesDomain(systemParams.getBaseUrl(), domainName)) {
                    return false;
                }
            }
            AdminSettings generalSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "general");
            String baseUrl = generalSettings.getJsonValue().get("baseUrl").asText();
            return !isBaseUrlMatchesDomain(baseUrl, domainName);
        } catch (Exception e) {
            log.warn("Failed to validate domain.", e);
            return false;
        }
    }

     private boolean isBaseUrlMatchesDomain(String baseUrl, String domainName) {
        String baseUrlDomainName = this.domainNameFromBaseUrl(baseUrl);
        return baseUrlDomainName != null && baseUrlDomainName.equalsIgnoreCase(domainName);
    }

    private String domainNameFromBaseUrl(String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            try {
                return URI.create(baseUrl).getHost();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private void saveEntityLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelParams) {
        if (loginWhiteLabelParams.getDomainId() == null) {
            throw new IncorrectParameterException("Domain id could not be empty");
        }
        Domain domain = domainService.findDomainById(tenantId, loginWhiteLabelParams.getDomainId());
        if (!isUsedOnSystemLevel(domain.getName())) {
            throw new IncorrectParameterException("Current domain name " + domain.getName() + " already used in the system level");
        }
        if (loginWhiteLabelParams.getBaseUrl() != null && !isValidUrl(loginWhiteLabelParams.getBaseUrl())) {
            throw new IncorrectParameterException("Base url " + loginWhiteLabelParams.getBaseUrl() + " is invalid");
        }
        saveLoginWhiteLabelParams(tenantId, customerId, loginWhiteLabelParams);
    }

    @Override
    public WhiteLabelingParams saveTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        saveWhiteLabelParams(tenantId, null, whiteLabelingParams);
        return getTenantWhiteLabelingParams(tenantId);
    }

    @Override
    public WhiteLabelingParams saveCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        saveWhiteLabelParams(tenantId, customerId, whiteLabelingParams);
        return getCustomerWhiteLabelingParams(tenantId, customerId);
    }

    @Override
    public WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams;
    }

    @Override
    public WhiteLabelingParams mergeTenantWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams.merge(getSystemWhiteLabelingParams());
    }

    @Override
    public WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        try {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer.isSubCustomer()) {
                whiteLabelingParams = getMergedCustomerHierarchyWhileLabelingParams(tenantId, customer.getParentCustomerId(), whiteLabelingParams);
            }
            WhiteLabelingParams tenantWhiteLabelingParams = getTenantWhiteLabelingParams(tenantId);
            whiteLabelingParams.setHideConnectivityDialog(tenantWhiteLabelingParams.getHideConnectivityDialog());
            return whiteLabelingParams.merge(tenantWhiteLabelingParams).merge(getSystemWhiteLabelingParams());
        } catch (Exception e) {
            log.error("Unable to merge Customer White Labeling Params!", e);
            throw new RuntimeException("Unable to merge Customer White Labeling Params!", e);
        }
    }

    @Override
    public void deleteAllTenantWhiteLabeling(TenantId tenantId) {
        for (WhiteLabelingType type : WhiteLabelingType.values()) {
            deleteWhiteLabeling(tenantId, null, type);
        }
    }

    private WhiteLabelingParams constructWlParams(JsonNode json, boolean isSystem) {
        WhiteLabelingParams result = null;
        if (json != null) {
            try {
                result = JacksonUtil.treeToValue(json, WhiteLabelingParams.class);
                if (isSystem) {
                    if (!json.has("helpLinkBaseUrl")) {
                        result.setHelpLinkBaseUrl("https://thingsboard.io");
                    }
                    if (!json.has("uiHelpBaseUrl")) {
                        result.setUiHelpBaseUrl(null);
                    }
                    if (!json.has("enableHelpLinks")) {
                        result.setEnableHelpLinks(true);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Unable to read White Labeling Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new WhiteLabelingParams();
            if (isSystem) {
                result.setHelpLinkBaseUrl("https://thingsboard.io");
                result.setUiHelpBaseUrl(null);
                result.setEnableHelpLinks(true);
            }
        }
        return result;
    }

    private WhiteLabelingParams getEntityWhiteLabelParams(TenantId tenantId, CustomerId customerId) {
        JsonNode jsonNode = null;
        if (isWhiteLabelingAllowed(tenantId, customerId)) {
            WhiteLabeling whiteLabeling = findByEntityId(tenantId, customerId, WhiteLabelingType.GENERAL);
            if (whiteLabeling != null) {
                jsonNode = whiteLabeling.getSettings();
            }
        }
        return constructWlParams(jsonNode, false);
    }

    private LoginWhiteLabelingParams constructLoginWlParams(WhiteLabeling whiteLabeling) {
        if (whiteLabeling == null || whiteLabeling.getSettings() == null) {
            return new LoginWhiteLabelingParams();
        }
        LoginWhiteLabelingParams  loginWLParams = JacksonUtil.treeToValue(whiteLabeling.getSettings(), LoginWhiteLabelingParams.class);
        loginWLParams.setDomainId(whiteLabeling.getDomainId());
        return loginWLParams;
    }

    private LoginWhiteLabelingParams getEntityLoginWhiteLabelParams(TenantId tenantId, CustomerId customerId) {
        WhiteLabeling whiteLabeling = null;
        if (isWhiteLabelingAllowed(tenantId, customerId)) {
            whiteLabeling = findByEntityId(tenantId, customerId, LOGIN);
        }
        return constructLoginWlParams(whiteLabeling);
    }

    public boolean isWhiteLabelingAllowed(TenantId tenantId, CustomerId customerId) {
        if (customerId != null && !customerId.isNullUid()) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer == null) {
                return false;
            }
            if (customer.isSubCustomer()) {
                if (isWhiteLabelingAllowed(tenantId, customer.getParentCustomerId())) {
                    return isWhiteLabelingAllowed(tenantId, customer.getCustomerId());
                } else {
                    return false;
                }
            } else {
                if (isCustomerWhiteLabelingAllowed(customer.getTenantId())) {
                    JsonNode allowWhiteLabelJsonNode = customer.getAdditionalInfo().get(ALLOW_WHITE_LABELING);
                    if (allowWhiteLabelJsonNode == null) {
                        return true;
                    } else {
                        return allowWhiteLabelJsonNode.asBoolean();
                    }
                } else {
                    return false;
                }
            }
        } else if (!tenantId.isSysTenantId()) {
            Tenant tenant = tenantService.findTenantById(tenantId);
            JsonNode allowWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_WHITE_LABELING) : null;
            if (allowWhiteLabelJsonNode == null) {
                return true;
            } else {
                return allowWhiteLabelJsonNode.asBoolean();
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean isCustomerWhiteLabelingAllowed(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        JsonNode allowWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_WHITE_LABELING) : null;
        if (allowWhiteLabelJsonNode == null) {
            return true;
        } else {
            if (allowWhiteLabelJsonNode.asBoolean()) {
                JsonNode allowCustomerWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_CUSTOMER_WHITE_LABELING) : null;
                if (allowCustomerWhiteLabelJsonNode == null) {
                    return true;
                } else {
                    return allowCustomerWhiteLabelJsonNode.asBoolean();
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isWhiteLabelingConfigured(TenantId tenantId) {
        return findByEntityId(tenantId, null, WhiteLabelingType.GENERAL) != null;
    }

    @Override
    public JsonNode saveMailTemplates(TenantId tenantId, JsonNode mailTemplates) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(tenantId);
        whiteLabeling.setType(WhiteLabelingType.MAIL_TEMPLATES);
        whiteLabeling.setSettings(mailTemplates);

        JsonNode result = whiteLabelingDao.save(tenantId, whiteLabeling).getSettings();
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(tenantId)
                .edgeEventType(EdgeEventType.MAIL_TEMPLATES).actionType(ActionType.UPDATED).build());
        return result;
    }

    @Override
    public JsonNode getCurrentTenantMailTemplates(TenantId tenantId, boolean systemByDefault) {
        JsonNode mailTemplatesSettings = findMailTemplatesByTenantId(tenantId, tenantId);
        if (mailTemplatesSettings.isEmpty()) {
            if (systemByDefault) {
                mailTemplatesSettings = findMailTemplatesByTenantId(tenantId, TenantId.SYS_TENANT_ID);
            }
        }
        return mailTemplatesSettings;
    }

    @Override
    public JsonNode findMailTemplatesByTenantId(TenantId tenantId, TenantId settingsTenantId) {
        if (settingsTenantId == null) {
            return JacksonUtil.newObjectNode();
        }
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(settingsTenantId, WhiteLabelingType.MAIL_TEMPLATES);
        WhiteLabeling mailTemplates = whiteLabelingDao.findById(tenantId, key);
        return mailTemplates == null ? JacksonUtil.newObjectNode() : mailTemplates.getSettings();
    }

    @Override
    public JsonNode getMergedTenantMailTemplates(TenantId tenantId) throws ThingsboardException {
        JsonNode mailTemplatesSettings = getCurrentTenantMailTemplates(tenantId, true);
        if (mailTemplatesSettings.isEmpty()) {
            throw new ThingsboardException("Failed to get tenant mail templates configuration. Settings not found!", ThingsboardErrorCode.GENERAL);
        }
        JsonNode useSystemMailSettingsNode = mailTemplatesSettings.get("useSystemMailSettings");
        if (useSystemMailSettingsNode == null || useSystemMailSettingsNode.asBoolean()) {
            JsonNode systemMailTemplates = findMailTemplatesByTenantId(tenantId, TenantId.SYS_TENANT_ID);
            if (systemMailTemplates.isEmpty()) {
                throw new ThingsboardException("Failed to get system mail templates configuration. Settings not found!", ThingsboardErrorCode.GENERAL);
            }
            mailTemplatesSettings = systemMailTemplates;
        }
        return mailTemplatesSettings;
    }

    private void saveLoginWhiteLabelParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams whiteLabelingParams) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(tenantId);
        whiteLabeling.setCustomerId(customerId);
        whiteLabeling.setType(LOGIN);
        whiteLabelingParams.setBaseUrl(StringUtils.toLowerCase(whiteLabelingParams.getBaseUrl()));
        whiteLabeling.setSettings(JacksonUtil.valueToTree(whiteLabelingParams));
        whiteLabeling.setDomainId(whiteLabelingParams.getDomainId());

        doSaveWhiteLabelingSettings(tenantId, whiteLabeling);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(getEntityIdForEvent(tenantId, customerId))
                .edgeEventType(EdgeEventType.LOGIN_WHITE_LABELING).actionType(ActionType.UPDATED).build());
    }

    private void saveWhiteLabelParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(tenantId);
        whiteLabeling.setCustomerId(customerId);
        whiteLabeling.setType(WhiteLabelingType.GENERAL);
        whiteLabeling.setSettings(JacksonUtil.valueToTree(whiteLabelingParams));

        doSaveWhiteLabelingSettings(tenantId, whiteLabeling);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(getEntityIdForEvent(tenantId, customerId))
                .edgeEventType(EdgeEventType.WHITE_LABELING).actionType(ActionType.UPDATED).build());
    }

    private static EntityId getEntityIdForEvent(TenantId tenantId, CustomerId customerId) {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    private WhiteLabeling doSaveWhiteLabelingSettings(TenantId tenantId, WhiteLabeling whiteLabeling) {
        try {
            imageService.replaceBase64WithImageUrl(whiteLabeling);
            WhiteLabeling saved = whiteLabelingDao.save(tenantId, whiteLabeling);
            WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(saved.getTenantId(), saved.getCustomerId(), saved.getType());
            publishEvictEvent(new WhiteLabelingEvictEvent(forKey(key)));
            if (whiteLabeling.getDomainId() != null) {
                Domain domain = domainService.findDomainById(tenantId, whiteLabeling.getDomainId());
                publishEvictEvent(new WhiteLabelingEvictEvent(forTypeAndDomain(whiteLabeling.getType(), domain.getName())));
            }
            return saved;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "white_labeling_domain_id_type_key", "Such domain name is already used for such type of white labeling settings!");
            throw t;
        }
    }

    @Override
    public WhiteLabeling findByEntityId(TenantId tenantId, CustomerId customerId, WhiteLabelingType type) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(tenantId, customerId, type);
        log.trace("Executing findById for key [{}] ", key);
        return cache.getAndPutInTransaction(forKey(key),
                () -> whiteLabelingDao.findById(tenantId,
                        key), true);
    }

    @Override
    public WhiteLabeling findWhiteLabelingByDomainAndType(String domainName, WhiteLabelingType type) {
        log.trace("Executing getWhiteLabeling for domain name [{}] and type [{}]", domainName, type);
        return cache.getAndPutInTransaction(forTypeAndDomain(type, domainName),
                () -> whiteLabelingDao.findByDomainAndType(TenantId.SYS_TENANT_ID, domainName, type), true);
    }

    @Override
    public WebSelfRegistrationParams saveTenantSelfRegistrationParams(TenantId tenantId, WebSelfRegistrationParams selfRegistrationParams) {
        if (selfRegistrationParams.getPrivacyPolicy() != null) {
            WhiteLabeling privacyPolicy = WhiteLabeling.builder()
                    .tenantId(tenantId)
                    .type(PRIVACY_POLICY)
                    .domainId(selfRegistrationParams.getDomainId())
                    .settings(JacksonUtil.newObjectNode().put("privacyPolicy", selfRegistrationParams.getPrivacyPolicy()))
                    .build();
            doSaveWhiteLabelingSettings(tenantId, privacyPolicy);
        }

        if (selfRegistrationParams.getTermsOfUse() != null) {
            WhiteLabeling termsOfUse = WhiteLabeling.builder()
                    .tenantId(tenantId)
                    .type(TERMS_OF_USE)
                    .domainId(selfRegistrationParams.getDomainId())
                    .settings(JacksonUtil.newObjectNode().put("termsOfUse", selfRegistrationParams.getTermsOfUse()))
                    .build();
            doSaveWhiteLabelingSettings(tenantId, termsOfUse);
        }

        selfRegistrationParams.setPrivacyPolicy(null);
        selfRegistrationParams.setTermsOfUse(null);

        WhiteLabeling selfRegistration = WhiteLabeling.builder()
                .tenantId(tenantId)
                .type(SELF_REGISTRATION)
                .domainId(selfRegistrationParams.getDomainId())
                .settings(JacksonUtil.valueToTree(selfRegistrationParams))
                .build();

        doSaveWhiteLabelingSettings(tenantId, selfRegistration);
        return getTenantSelfRegistrationParams(tenantId);
    }

    @Override
    public WebSelfRegistrationParams getTenantSelfRegistrationParams(TenantId tenantId) {
        WhiteLabeling whiteLabeling = findByEntityId(tenantId, null, SELF_REGISTRATION);
        return constructSRParams(whiteLabeling);
    }

    private WebSelfRegistrationParams constructSRParams(WhiteLabeling whiteLabeling) {
        if (whiteLabeling == null || whiteLabeling.getSettings() == null) {
            return null;
        }
        WebSelfRegistrationParams selfRegistrationParams =  JacksonUtil.treeToValue(whiteLabeling.getSettings(), WebSelfRegistrationParams.class);
        selfRegistrationParams.setDomainId(whiteLabeling.getDomainId());
        return selfRegistrationParams;
    }

    @Override
    public WebSelfRegistrationParams getWebSelfRegistrationParams(String domainName) {
        WhiteLabeling whiteLabeling = findWhiteLabelingByDomainAndType(domainName, SELF_REGISTRATION);
        return whiteLabeling != null ? JacksonUtil.treeToValue(whiteLabeling.getSettings(), WebSelfRegistrationParams.class) : null;
    }

    @Override
    public JsonNode getWebPrivacyPolicy(String domainName) {
        WhiteLabeling whiteLabeling = findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.PRIVACY_POLICY);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public JsonNode getTenantPrivacyPolicy(TenantId tenantId) {
        WhiteLabeling whiteLabeling = findByEntityId(tenantId, null, WhiteLabelingType.PRIVACY_POLICY);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public JsonNode getWebTermsOfUse(String domainName) {
        WhiteLabeling whiteLabeling = findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.TERMS_OF_USE);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public JsonNode getTenantTermsOfUse(TenantId tenantId) {
        WhiteLabeling whiteLabeling = findByEntityId(tenantId, null, WhiteLabelingType.TERMS_OF_USE);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public void deleteWhiteLabeling(TenantId tenantId, CustomerId customerId, WhiteLabelingType type) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(tenantId, customerId, type);
        WhiteLabeling whiteLabeling = whiteLabelingDao.findById(tenantId, key);
        if (whiteLabeling != null) {
            whiteLabelingDao.removeById(tenantId, key);
            publishEvictEvent(new WhiteLabelingEvictEvent(forKey(key)));
            if (whiteLabeling.getDomainId() != null) {
                Domain domain = domainService.findDomainById(tenantId, whiteLabeling.getDomainId());
                publishEvictEvent(new WhiteLabelingEvictEvent(forTypeAndDomain(type, domain.getName())));
            }
            EdgeEventType edgeEventType = type.equals(LOGIN) ? EdgeEventType.LOGIN_WHITE_LABELING : EdgeEventType.WHITE_LABELING;
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(getEntityIdForEvent(tenantId, customerId))
                    .edgeEventType(edgeEventType).actionType(ActionType.DELETED).body(JacksonUtil.toString(whiteLabeling)).build());
        }
    }

    @TransactionalEventListener(classes = WhiteLabelingEvictEvent.class)
    @Override
    public void handleEvictEvent(WhiteLabelingEvictEvent event) {
        cache.evict(event.getKey());
    }
}
