/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.oauth2.OAuth2Utils.*;

@Slf4j
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ReentrantLock clientRegistrationSaveLock = new ReentrantLock();

    @Autowired
    private Environment environment;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TenantService tenantService;

    private boolean isInstall() {
        return environment.acceptsProfiles("install");
    }

    @Override
    public Pair<TenantId, OAuth2ClientRegistration> getClientRegistrationWithTenant(String registrationId) {
        return getAllOAuth2ClientsParams().entrySet().stream()
                .map(entry -> {
                    TenantId tenantId = entry.getKey();
                    OAuth2ClientRegistration clientRegistration = toClientRegistrationStream(entry.getValue())
                            .filter(registration -> registrationId.equals(registration.getRegistrationId()))
                            .findFirst()
                            .orElse(null);
                    return clientRegistration != null ?
                            ImmutablePair.of(tenantId, clientRegistration) : null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null)
                ;
    }

    @Override
    public OAuth2ClientRegistration getClientRegistration(String registrationId) {
        Pair<TenantId, OAuth2ClientRegistration> clientRegistrationPair = getClientRegistrationWithTenant(registrationId);
        return clientRegistrationPair != null ? clientRegistrationPair.getRight() : null;
    }

    @Override
    public List<OAuth2ClientInfo> getOAuth2Clients(String domainName) {
        OAuth2ClientsDomainParams oAuth2ClientsDomainParams = getMergedOAuth2ClientsParams(domainName);
        return oAuth2ClientsDomainParams != null && oAuth2ClientsDomainParams.getClientRegistrations() != null ?
                oAuth2ClientsDomainParams.getClientRegistrations().stream()
                        .map(OAuth2Utils::toClientInfo)
                        .collect(Collectors.toList())
                : Collections.emptyList()
                ;
    }

    @Override
    public OAuth2ClientsParams saveSystemOAuth2ClientsParams(OAuth2ClientsParams oAuth2ClientsParams) {
        validate(oAuth2ClientsParams);

        validateRegistrationIdUniqueness(oAuth2ClientsParams, TenantId.SYS_TENANT_ID);
        clientRegistrationSaveLock.lock();
        try {
            validateRegistrationIdUniqueness(oAuth2ClientsParams, TenantId.SYS_TENANT_ID);
            AdminSettings oauth2SystemAdminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
            if (oauth2SystemAdminSettings == null) {
                oauth2SystemAdminSettings = createSystemAdminSettings();
            }
            String json = toJson(oAuth2ClientsParams);
            ((ObjectNode) oauth2SystemAdminSettings.getJsonValue()).put(SYSTEM_SETTINGS_OAUTH2_VALUE, json);
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, oauth2SystemAdminSettings);
        } finally {
            clientRegistrationSaveLock.unlock();
        }

        return getSystemOAuth2ClientsParams();
    }

    @Override
    public OAuth2ClientsParams saveTenantOAuth2ClientsParams(TenantId tenantId, OAuth2ClientsParams oAuth2ClientsParams) {
        if (oAuth2ClientsParams.getClientsDomainsParams().size() != 1) {
            throw new DataValidationException("Tenant can configure OAuth2 only for one domain!");
        }
        validate(oAuth2ClientsParams);

        validateRegistrationIdUniqueness(oAuth2ClientsParams, tenantId);
        clientRegistrationSaveLock.lock();
        try {
            validateRegistrationIdUniqueness(oAuth2ClientsParams, tenantId);

            OAuth2ClientsDomainParams oAuth2ClientsDomainParams = oAuth2ClientsParams.getClientsDomainsParams().get(0);
            String adminSettingsId = processTenantAdminSettings(tenantId, oAuth2ClientsDomainParams.getDomainName(), oAuth2ClientsDomainParams.getAdminSettingsId());
            oAuth2ClientsDomainParams.setAdminSettingsId(adminSettingsId);

            List<AttributeKvEntry> attributes = createOAuth2ClientsParamsAttributes(oAuth2ClientsParams);
            try {
                attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, attributes).get();
            } catch (Exception e) {
                log.error("Unable to save OAuth2 Client Registration Params to attributes!", e);
                throw new IncorrectParameterException("Unable to save OAuth2 Client Registration Params to attributes!");
            }

        } finally {
            clientRegistrationSaveLock.unlock();
        }

        return getTenantOAuth2ClientsParams(tenantId);
    }

    private List<AttributeKvEntry> createOAuth2ClientsParamsAttributes(OAuth2ClientsParams oAuth2ClientsParams) {
        String json = toJson(oAuth2ClientsParams);
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        attributes.add(new BaseAttributeKvEntry(new StringDataEntry(OAUTH2_CLIENT_REGISTRATIONS_PARAMS, json), ts));
        return attributes;
    }

    private String processTenantAdminSettings(TenantId tenantId, String domainName, String prevAdminSettingsId) {
        String selectedDomainSettingsKey = constructAdminSettingsDomainKey(domainName);
        AdminSettings existentAdminSettingsByKey = adminSettingsService.findAdminSettingsByKey(tenantId, selectedDomainSettingsKey);
        if (StringUtils.isEmpty(prevAdminSettingsId)) {
            if (existentAdminSettingsByKey == null) {
                AdminSettings tenantAdminSettings = createTenantAdminSettings(tenantId, selectedDomainSettingsKey);
                existentAdminSettingsByKey = adminSettingsService.saveAdminSettings(tenantId, tenantAdminSettings);
                return existentAdminSettingsByKey.getId().getId().toString();
            } else {
                log.error("Current domain name [{}] already registered in the system!", domainName);
                throw new IncorrectParameterException("Current domain name [" + domainName + "] already registered in the system!");
            }
        } else {
            AdminSettings existentOAuth2ClientsSettingsById = adminSettingsService.findAdminSettingsById(
                    tenantId,
                    new AdminSettingsId(UUID.fromString(prevAdminSettingsId))
            );

            if (existentOAuth2ClientsSettingsById == null) {
                log.error("Admin setting ID is already set in OAuth2 Client Params object, but doesn't exist in the database");
                throw new IllegalStateException("Admin setting ID is already set in OAuth2 Client Params object, but doesn't exist in the database");
            }

            if (!existentOAuth2ClientsSettingsById.getKey().equals(selectedDomainSettingsKey)) {
                if (existentAdminSettingsByKey == null) {
                    AdminSettings newOAuth2ClientsSettings = replaceExistentAdminSettings(tenantId, selectedDomainSettingsKey, existentOAuth2ClientsSettingsById.getKey());
                    return newOAuth2ClientsSettings.getId().getId().toString();
                } else {
                    log.error("Current domain name [{}] already registered in the system!", domainName);
                    throw new IncorrectParameterException("Current domain name [" + domainName + "] already registered in the system!");
                }
            }
            return prevAdminSettingsId;
        }
    }

    private AdminSettings replaceExistentAdminSettings(TenantId tenantId, String newKey, String oldKey) {
        adminSettingsService.deleteAdminSettingsByKey(tenantId, oldKey);
        AdminSettings tenantAdminSettings = createTenantAdminSettings(tenantId, newKey);
        return adminSettingsService.saveAdminSettings(tenantId, tenantAdminSettings);
    }

    private AdminSettings createTenantAdminSettings(TenantId tenantId, String clientRegistrationsKey) {
        AdminSettings clientRegistrationParamsSettings = new AdminSettings();
        clientRegistrationParamsSettings.setKey(clientRegistrationsKey);
        ObjectNode node = mapper.createObjectNode();
        node.put("entityType", EntityType.TENANT.name());
        node.put("entityId", tenantId.toString());
        clientRegistrationParamsSettings.setJsonValue(node);
        return clientRegistrationParamsSettings;
    }

    private AdminSettings createSystemAdminSettings() {
        AdminSettings clientRegistrationParamsSettings = new AdminSettings();
        clientRegistrationParamsSettings.setKey(OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
        ObjectNode clientRegistrationsNode = mapper.createObjectNode();

        clientRegistrationParamsSettings.setJsonValue(clientRegistrationsNode);

        return clientRegistrationParamsSettings;
    }

    private void validateRegistrationIdUniqueness(OAuth2ClientsParams inputOAuth2ClientsParams, TenantId tenantId) {
        List<String> registrationIds = toClientRegistrationStream(inputOAuth2ClientsParams)
                .map(OAuth2ClientRegistration::getRegistrationId)
                .collect(Collectors.toList());

        boolean regIdDuplicates = registrationIds.stream()
                .anyMatch(registrationId -> Collections.frequency(registrationIds, registrationId) > 1);
        if (regIdDuplicates) {
            throw new DataValidationException("All registration IDs should be unique!");
        }

        getAllOAuth2ClientsParams().forEach((paramsTenantId, oAuth2ClientsParams) -> {
            if (tenantId.equals(paramsTenantId)) return;
            Set<String> duplicatedRegistrationIds = toClientRegistrationStream(oAuth2ClientsParams)
                    .map(OAuth2ClientRegistration::getRegistrationId)
                    .filter(registrationIds::contains)
                    .collect(Collectors.toSet());
            if (!duplicatedRegistrationIds.isEmpty()) {
                log.error("RegistrationIds [{}] are already registered in the system!", duplicatedRegistrationIds);
                throw new IncorrectParameterException("RegistrationIds [" + duplicatedRegistrationIds + "] are already registered in the system!");
            }
        });
    }

    private void validate(OAuth2ClientsParams oAuth2ClientsParams) {
        validateDomainNames(oAuth2ClientsParams);
        validateAdminSettingsIds(oAuth2ClientsParams);

        toClientRegistrationStream(oAuth2ClientsParams)
                .forEach(validator);
    }

    private void validateDomainNames(OAuth2ClientsParams oAuth2ClientsParams) {
        List<String> domainNames = oAuth2ClientsParams.getClientsDomainsParams().stream()
                .map(OAuth2ClientsDomainParams::getDomainName)
                .collect(Collectors.toList());

        domainNames.forEach(domainName -> {
            if (StringUtils.isEmpty(domainName)) {
                throw new DataValidationException("Domain name should be specified!");
            }
        });

        boolean duplicateDomainNames = domainNames.stream()
                .anyMatch(domainName -> Collections.frequency(domainNames, domainName) > 1);
        if (duplicateDomainNames) {
            throw new DataValidationException("All domain names should be unique!");
        }
    }

    private void validateAdminSettingsIds(OAuth2ClientsParams oAuth2ClientsParams) {
        List<String> adminSettingsIds = oAuth2ClientsParams.getClientsDomainsParams().stream()
                .map(OAuth2ClientsDomainParams::getAdminSettingsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean duplicateAdminSettingsIds = adminSettingsIds.stream()
                .anyMatch(adminSettingsId -> Collections.frequency(adminSettingsIds, adminSettingsId) > 1);
        if (duplicateAdminSettingsIds) {
            throw new DataValidationException("All admin settings ids should be unique!");
        }
    }

    @Override
    public OAuth2ClientsParams getSystemOAuth2ClientsParams() {
        AdminSettings oauth2ClientsParamsSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
        String json = null;
        if (oauth2ClientsParamsSettings != null) {
            json = oauth2ClientsParamsSettings.getJsonValue().get(SYSTEM_SETTINGS_OAUTH2_VALUE).asText();
        }
        return constructOAuth2ClientsParams(json);
    }

    @Override
    public OAuth2ClientsParams getTenantOAuth2ClientsParams(TenantId tenantId) {
        ListenableFuture<String> jsonFuture;
        if (isOAuth2ClientRegistrationAllowed(tenantId)) {
            jsonFuture = getOAuth2ClientsParamsAttribute(tenantId);
        } else {
            jsonFuture = Futures.immediateFuture("");
        }
        try {
            return Futures.transform(jsonFuture, this::constructOAuth2ClientsParams, MoreExecutors.directExecutor()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to read OAuth2 Clients Params from attributes!", e);
            throw new RuntimeException("Failed to read OAuth2 Clients Params from attributes!", e);
        }
    }

    // TODO this is just for test, maybe there's a better way to test it without exporting to interface
    @Override
    public Map<TenantId, OAuth2ClientsParams> getAllOAuth2ClientsParams() {
        OAuth2ClientsParams systemOAuth2ClientsParams = getSystemOAuth2ClientsParams();
        ListenableFuture<Map<String, String>> jsonFuture = getAllOAuth2ClientsParamsAttribute();
        try {
            return Futures.transform(jsonFuture,
                    clientsParamsByKvEntryKey -> {
                        Map<TenantId, OAuth2ClientsParams> tenantClientParams = clientsParamsByKvEntryKey != null ?
                                clientsParamsByKvEntryKey.entrySet().stream()
                                        .collect(Collectors.toMap(
                                                entry -> new TenantId(UUIDConverter.fromString(entry.getKey())),
                                                entry -> constructOAuth2ClientsParams(entry.getValue())
                                        ))
                                : new HashMap<>();
                        if (systemOAuth2ClientsParams.getClientsDomainsParams() != null) {
                            tenantClientParams.put(TenantId.SYS_TENANT_ID, systemOAuth2ClientsParams);
                        }
                        return tenantClientParams;
                    },
                    MoreExecutors.directExecutor()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to read OAuth2 Clients Params from attributes!", e);
            throw new RuntimeException("Failed to read OAuth2 Clients Params from attributes!", e);
        }
    }

    @Override
    public void deleteTenantOAuth2ClientsParams(TenantId tenantId) {
        OAuth2ClientsParams params = getTenantOAuth2ClientsParams(tenantId);
        if (params == null || params.getClientsDomainsParams() == null) return;
        OAuth2ClientsDomainParams domainParams = params.getClientsDomainsParams().get(0);
        String settingsKey = constructAdminSettingsDomainKey(domainParams.getDomainName());
        adminSettingsService.deleteAdminSettingsByKey(tenantId, settingsKey);
        attributesService.removeAll(tenantId, tenantId, DataConstants.SERVER_SCOPE, Collections.singletonList(OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
    }

    @Override
    public void deleteSystemOAuth2ClientsParams() {
        adminSettingsService.deleteAdminSettingsByKey(TenantId.SYS_TENANT_ID, OAuth2Utils.OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
    }

    @Override
    public boolean isOAuth2ClientRegistrationAllowed(TenantId tenantId) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        if (tenant == null) return false;
        JsonNode allowOAuth2ConfigurationJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_OAUTH2_CONFIGURATION) : null;
        if (allowOAuth2ConfigurationJsonNode == null) {
            return true;
        } else {
            return allowOAuth2ConfigurationJsonNode.asBoolean();
        }
    }

    private ListenableFuture<String> getOAuth2ClientsParamsAttribute(TenantId tenantId) {
        ListenableFuture<List<AttributeKvEntry>> attributeKvEntriesFuture;
        try {
            attributeKvEntriesFuture = attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE,
                    Collections.singletonList(OAUTH2_CLIENT_REGISTRATIONS_PARAMS));
        } catch (Exception e) {
            log.error("Unable to read OAuth2 Clients Params from attributes!", e);
            throw new IncorrectParameterException("Unable to read OAuth2 Clients Params from attributes!");
        }
        return Futures.transform(attributeKvEntriesFuture, attributeKvEntries -> {
            if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                AttributeKvEntry kvEntry = attributeKvEntries.get(0);
                return kvEntry.getValueAsString();
            } else {
                return "";
            }
        }, MoreExecutors.directExecutor());
    }

    // TODO maybe it's better to load all tenants and get attribute for each one
    private ListenableFuture<Map<String, String>> getAllOAuth2ClientsParamsAttribute() {
        ListenableFuture<List<EntityAttributeKvEntry>> entityAttributeKvEntriesFuture;
        try {
            entityAttributeKvEntriesFuture = attributesService.findAllByAttributeKey(OAUTH2_CLIENT_REGISTRATIONS_PARAMS);
        } catch (Exception e) {
            log.error("Unable to read OAuth2 Clients Params from attributes!", e);
            throw new IncorrectParameterException("Unable to read OAuth2 Clients Params from attributes!");
        }
        return Futures.transform(entityAttributeKvEntriesFuture, attributeKvEntries -> {
            if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                return attributeKvEntries.stream()
                        .collect(Collectors.toMap(EntityAttributeKvEntry::getEntityId, EntityAttributeKvEntry::getValueAsString));
            } else {
                return Collections.emptyMap();
            }
        }, MoreExecutors.directExecutor());
    }

    private OAuth2ClientsDomainParams getMergedOAuth2ClientsParams(String domainName) {
        OAuth2ClientsDomainParams result = OAuth2ClientsDomainParams.builder()
                .domainName(domainName)
                .clientRegistrations(new ArrayList<>())
                .build();

        OAuth2ClientsParams systemOAuth2ClientsParams = getSystemOAuth2ClientsParams();
        OAuth2ClientsDomainParams systemOAuth2ClientsDomainParams = systemOAuth2ClientsParams != null && systemOAuth2ClientsParams.getClientsDomainsParams() != null ?
                systemOAuth2ClientsParams.getClientsDomainsParams().stream()
                        .filter(oAuth2ClientsDomainParams -> domainName.equals(oAuth2ClientsDomainParams.getDomainName()))
                        .findFirst()
                        .orElse(null)
                : null;

        result = mergeDomainParams(result, systemOAuth2ClientsDomainParams);

        AdminSettings oauth2ClientsSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, constructAdminSettingsDomainKey(domainName));
        if (oauth2ClientsSettings != null) {
            String strEntityType = oauth2ClientsSettings.getJsonValue().get("entityType").asText();
            String strEntityId = oauth2ClientsSettings.getJsonValue().get("entityId").asText();
            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            if (!entityId.getEntityType().equals(EntityType.TENANT)) {
                log.error("Only tenant can configure OAuth2 for certain domain!");
                throw new IllegalStateException("Only tenant can configure OAuth2 for certain domain!");
            }
            TenantId tenantId = (TenantId) entityId;
            OAuth2ClientsParams tenantOAuth2ClientsParams = getTenantOAuth2ClientsParams(tenantId);
            OAuth2ClientsDomainParams tenantDomainsParams = tenantOAuth2ClientsParams != null && tenantOAuth2ClientsParams.getClientsDomainsParams() != null ?
                    tenantOAuth2ClientsParams.getClientsDomainsParams().stream().findFirst().orElse(null) : null;
            result = mergeDomainParams(result, tenantDomainsParams);
        }
        return result;
    }

    private OAuth2ClientsDomainParams mergeDomainParams(OAuth2ClientsDomainParams sourceParams, OAuth2ClientsDomainParams newParams){
        if (newParams == null) return sourceParams;

        OAuth2ClientsDomainParams.OAuth2ClientsDomainParamsBuilder mergedParamsBuilder = sourceParams.toBuilder();

        if (newParams.getClientRegistrations() != null){
            List<OAuth2ClientRegistration> mergedClientRegistrations = sourceParams.getClientRegistrations() != null ?
                    sourceParams.getClientRegistrations() : new ArrayList<>();
            mergedClientRegistrations.addAll(newParams.getClientRegistrations());
            mergedParamsBuilder.clientRegistrations(mergedClientRegistrations);
        }

        if (newParams.getAdminSettingsId() != null){
            mergedParamsBuilder.adminSettingsId(newParams.getAdminSettingsId());
        }

        return mergedParamsBuilder.build();
    }

    private OAuth2ClientsParams constructOAuth2ClientsParams(String json) {
        OAuth2ClientsParams result = null;
        if (!StringUtils.isEmpty(json)) {
            try {
                result = mapper.readValue(json, OAuth2ClientsParams.class);
            } catch (IOException e) {
                log.error("Unable to read OAuth2 Clients Params from JSON!", e);
                throw new IncorrectParameterException("Unable to read OAuth2 Clients Params from JSON!");
            }
        }
        if (result == null) {
            result = new OAuth2ClientsParams();
        }
        return result;
    }

    private String toJson(OAuth2ClientsParams oAuth2ClientsParams) {
        String json;
        try {
            json = mapper.writeValueAsString(oAuth2ClientsParams);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert OAuth2 Client Registration Params to JSON!", e);
            throw new IncorrectParameterException("Unable to convert OAuth2 Client Registration Params to JSON!");
        }
        return json;
    }

    private final Consumer<OAuth2ClientRegistration> validator = clientRegistration -> {
        if (StringUtils.isEmpty(clientRegistration.getRegistrationId())) {
            throw new DataValidationException("Registration ID should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientId())) {
            throw new DataValidationException("Client ID should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientSecret())) {
            throw new DataValidationException("Client secret should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getAuthorizationUri())) {
            throw new DataValidationException("Authorization uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getTokenUri())) {
            throw new DataValidationException("Token uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getRedirectUriTemplate())) {
            throw new DataValidationException("Redirect uri template should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getScope())) {
            throw new DataValidationException("Scope should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getUserInfoUri())) {
            throw new DataValidationException("User info uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getUserNameAttributeName())) {
            throw new DataValidationException("User name attribute name should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getJwkSetUri())) {
            throw new DataValidationException("Jwk set uri should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientAuthenticationMethod())) {
            throw new DataValidationException("Client authentication method should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getClientName())) {
            throw new DataValidationException("Client name should be specified!");
        }
        if (StringUtils.isEmpty(clientRegistration.getLoginButtonLabel())) {
            throw new DataValidationException("Login button label should be specified!");
        }
        OAuth2MapperConfig mapperConfig = clientRegistration.getMapperConfig();
        if (mapperConfig == null) {
            throw new DataValidationException("Mapper config should be specified!");
        }
        if (mapperConfig.getType() == null) {
            throw new DataValidationException("Mapper config type should be specified!");
        }
        if (mapperConfig.getType() == MapperType.BASIC) {
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasicConfig();
            if (basicConfig == null) {
                throw new DataValidationException("Basic config should be specified!");
            }
            if (StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                throw new DataValidationException("Email attribute key should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == null) {
                throw new DataValidationException("Tenant name strategy should be specified!");
            }
            if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                    && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                throw new DataValidationException("Tenant name pattern should be specified!");
            }
        }
        if (mapperConfig.getType() == MapperType.CUSTOM) {
            OAuth2CustomMapperConfig customConfig = mapperConfig.getCustomConfig();
            if (customConfig == null) {
                throw new DataValidationException("Custom config should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getUrl())) {
                throw new DataValidationException("Custom mapper URL should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getUsername())) {
                throw new DataValidationException("Custom mapper username should be specified!");
            }
            if (StringUtils.isEmpty(customConfig.getPassword())) {
                throw new DataValidationException("Custom mapper password should be specified!");
            }
        }
    };
}
