// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.JacksonUtil.extractKeys;
import static org.thingsboard.common.util.JacksonUtil.merge;
import static org.thingsboard.common.util.JacksonUtil.newObjectNode;
import static org.thingsboard.server.common.data.wl.WhiteLabelingType.LOGIN;

@Service
@RequiredArgsConstructor
public class DefaultTranslationService implements TranslationService {

    public static final String LOCALE_FILES_DIRECTORY_PATH = "public/assets/locale";
    public static final Pattern LOCALE_FILE_PATTERN = Pattern.compile("locale\\.constant-(.*?)\\.json");
    public static final String DEFAULT_LOCALE_CODE = "en_US";
    public static final Set<String> DEFAULT_LOCALE_KEYS;
    private static final Map<String, JsonNode> TRANSLATION_VALUE_MAP = new HashMap<>();
    private static final Map<String, TranslationInfo> TRANSLATION_INFO_MAP = new HashMap<>();

    private final CustomTranslationService customTranslationService;
    private final WhiteLabelingService whiteLabelingService;
    private final CustomerService customerService;

    static {
        JsonNode defaultTranslation = readResourceLocaleTranslation(DEFAULT_LOCALE_CODE);
        DEFAULT_LOCALE_KEYS = extractKeys(defaultTranslation);

        Set<String> systemLocaleCodes = getAvailableResourceLocaleCodes();
        for (String localeCode : systemLocaleCodes) {
            JsonNode resourceLocaleTranslation = readResourceLocaleTranslation(localeCode);
            TRANSLATION_INFO_MAP.put(localeCode, createTranslationInfo(DEFAULT_LOCALE_KEYS, localeCode, resourceLocaleTranslation, false));
            TRANSLATION_VALUE_MAP.put(localeCode, merge(defaultTranslation.deepCopy(), resourceLocaleTranslation));
        }
    }

    @Override
    public List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId) {
        Map<String, TranslationInfo> translationInfos = JacksonUtil.clone(TRANSLATION_INFO_MAP);

        Set<String> customizedLocales = getMergedCustomizedLocales(tenantId, customerId);
        Set<String> currentCustomizedLocales = customTranslationService.getCurrentCustomizedLocales(tenantId, customerId);

        Set<String> engLocaleKeys = extractKeys(getFullTranslation(tenantId, customerId, DEFAULT_LOCALE_CODE));
        for (String customizedLocale : customizedLocales) {
            JsonNode customTranslation = getMergedCustomTranslation(tenantId, customerId, customizedLocale);
            if (translationInfos.containsKey(customizedLocale)) {
                JsonNode resourceTranslation = readResourceLocaleTranslation(customizedLocale);
                customTranslation = merge(resourceTranslation, customTranslation);
            }
            boolean customized = currentCustomizedLocales.contains(customizedLocale);
            translationInfos.put(customizedLocale, createTranslationInfo(engLocaleKeys, customizedLocale, customTranslation, customized));
        }
        return new ArrayList<>(translationInfos.values());
    }

    @Override
    public Set<String> getAvailableLocaleCodes(TenantId tenantId, CustomerId customerId) {
        Set<String> availableLocaleCodes = new HashSet<>(TRANSLATION_INFO_MAP.keySet());
        Set<String> customizedLocales = getMergedCustomizedLocales(tenantId, customerId);
        availableLocaleCodes.addAll(customizedLocales);
        return availableLocaleCodes;
    }

    @Override
    public JsonNode getLoginTranslation(String localeCode, String domainName) {
        WhiteLabeling whiteLabeling = whiteLabelingService.findWhiteLabelingByDomainAndType(domainName, LOGIN);
        JsonNode fullTranslation;
        if (whiteLabeling != null) {
            TenantId tenantId = whiteLabeling.getTenantId();
            CustomerId customerId = whiteLabeling.getCustomerId();
            fullTranslation = getFullTranslation(tenantId, customerId, localeCode);
        } else {
            fullTranslation = TRANSLATION_VALUE_MAP.getOrDefault(localeCode, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE)).deepCopy();
        }
        ObjectNode loginPageTranslation = newObjectNode();
        ObjectNode securityNode = newObjectNode();
        securityNode.set("2fa", fullTranslation.get("security").get("2fa"));
        securityNode.set("password-requirement", fullTranslation.get("security").get("password-requirement"));
        loginPageTranslation.set("login", fullTranslation.get("login"));
        loginPageTranslation.set("signup", fullTranslation.get("signup"));
        loginPageTranslation.set("common", fullTranslation.get("common"));
        loginPageTranslation.set("action", fullTranslation.get("action"));
        loginPageTranslation.set("security", securityNode);
        loginPageTranslation.set("access", fullTranslation.get("access"));
        loginPageTranslation.set("user", newObjectNode().set("invalid-email-format", fullTranslation.get("user").get("invalid-email-format")));
        return loginPageTranslation;
    }

    @Override
    public JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation = getMergedCustomTranslation(tenantId, customerId, localeCode);
        JsonNode defaultTranslation = TRANSLATION_VALUE_MAP.getOrDefault(localeCode, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE)).deepCopy();
        JsonNode merged = merge(defaultTranslation, customTranslation);

        // add new keys from default locale custom translation
        JsonNode defaultCustomTranslation = getMergedCustomTranslation(tenantId, customerId, DEFAULT_LOCALE_CODE);
        addNonExisting(merged, defaultCustomTranslation);
        return merged;
    }

    @Override
    public JsonNode getTranslationForBasicEdit(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode fullTranslation = getFullTranslation(tenantId, customerId, localeCode).deepCopy();
        JsonNode currentCustomTranslation = customTranslationService.getCurrentCustomTranslation(tenantId, customerId, localeCode);
        JsonNode originalTranslation = getTranslatedOnly(tenantId, customerId, DEFAULT_LOCALE_CODE, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE).deepCopy());
        JsonNode parentOriginalTranslation = getParentTranslatedOnly(tenantId, customerId, DEFAULT_LOCALE_CODE, TRANSLATION_VALUE_MAP.get(DEFAULT_LOCALE_CODE).deepCopy());
        JsonNode resourceTranslation = TRANSLATION_VALUE_MAP.containsKey(localeCode) ? readResourceLocaleTranslation(localeCode) : JacksonUtil.newObjectNode();
        JsonNode translated = getTranslatedOnly(tenantId, customerId, localeCode, resourceTranslation.deepCopy());
        JsonNode parentTranslated = getParentTranslatedOnly(tenantId, customerId, localeCode, resourceTranslation.deepCopy());
        buildTranslationInfoForEdit(fullTranslation, translated, parentTranslated, originalTranslation, currentCustomTranslation, parentOriginalTranslation);
        return fullTranslation;
    }

    private static TranslationInfo createTranslationInfo(Set<String> engLocaleKeys, String localeCode, JsonNode translation, boolean customized) {
        int progress = calculateTranslationProgress(engLocaleKeys, translation);
        Locale locale = Locale.forLanguageTag(localeCode.replace("_", "-"));
        return TranslationInfo.builder()
                .customized(customized)
                .localeCode(localeCode)
                .country(locale.getDisplayCountry())
                .language(StringUtils.capitalize(locale.getDisplayLanguage(locale) + " (" + locale.getDisplayLanguage() + ")"))
                .progress(progress)
                .build();
    }

    private JsonNode getMergedCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation;
        if (tenantId.isSysTenantId()) {
            customTranslation = customTranslationService.getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode);
        } else if (customerId == null || customerId.isNullUid()) {
            customTranslation = customTranslationService.getMergedTenantCustomTranslation(tenantId, localeCode);
        } else {
            customTranslation = customTranslationService.getMergedCustomerCustomTranslation(tenantId, customerId, localeCode);
        }
        return customTranslation;
    }

    private Set<String> getMergedCustomizedLocales(TenantId tenantId, CustomerId customerId) {
        Set<String> customizedLocales;
        if (tenantId.isSysTenantId()) {
            customizedLocales = customTranslationService.getCurrentCustomizedLocales(TenantId.SYS_TENANT_ID, null);
        } else if (customerId == null || customerId.isNullUid()) {
            customizedLocales = customTranslationService.getMergedTenantCustomizedLocales(tenantId);
        } else {
            customizedLocales = customTranslationService.getMergedCustomerCustomizedLocales(tenantId, customerId);
        }
        return customizedLocales;
    }

    private static JsonNode readResourceLocaleTranslation(String localeCode) {
        String filePath = LOCALE_FILES_DIRECTORY_PATH + "/locale.constant-" + localeCode + ".json";
        try (InputStream in = DefaultTbCustomTranslationService.class.getClassLoader().getResourceAsStream(filePath)) {
            return JacksonUtil.OBJECT_MAPPER.readTree(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read locale translation for " + localeCode + "!", e);
        }
    }

    private static Set<String> getAvailableResourceLocaleCodes() {
        List<String> filenames = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:public/assets/locale/*.json");
            Arrays.stream(resources).forEach(resource -> filenames.add(resource.getFilename()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get list of system locales!", e);
        }
        return filenames.stream().map(DefaultTranslationService::getLocaleFromFileName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String getLocaleFromFileName(String filename) {
        Matcher matcher = LOCALE_FILE_PATTERN.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private static int calculateTranslationProgress(Set<String> defaultLocaleKeys, JsonNode translation) {
        Set<String> localeKeys = extractKeys(translation);
        long translated = defaultLocaleKeys.stream()
                .filter(localeKeys::contains)
                .count();
        return (int) (((translated) * 100) / defaultLocaleKeys.size());
    }

    private void buildTranslationInfoForEdit(JsonNode fullTranslation, JsonNode translated, JsonNode parentTranslated, JsonNode original, JsonNode custom, JsonNode parentOrigin) {
        Iterator<String> fieldNamesIterator = fullTranslation.fieldNames();
        while (fieldNamesIterator.hasNext()) {
            String fieldName = fieldNamesIterator.next();
            JsonNode fullNode = fullTranslation.get(fieldName);
            JsonNode customNode = custom == null ? null : custom.get(fieldName);
            JsonNode parentTranslatedNode = parentTranslated == null ? null : parentTranslated.get(fieldName);
            JsonNode originNode = original == null ? null : original.get(fieldName);
            JsonNode translatedNode = translated == null ? null : translated.get(fieldName);
            JsonNode parentOriginNode = parentOrigin == null ? null : parentOrigin.get(fieldName);
            if (fullNode.isObject()) {
                buildTranslationInfoForEdit(fullNode, translatedNode, parentTranslatedNode, originNode, customNode, parentOriginNode);
            } else {
                ObjectNode info = newObjectNode();
                // original translation
                if (originNode != null) {
                    info.put("o", originNode.asText());
                }
                // parent translation
                if (parentTranslatedNode != null) {
                    info.put("p", parentTranslatedNode.asText());
                }
                if (customNode != null) {
                    info.put("s", "C"); // customized key
                    info.put("t", customNode.asText());
                } else if (translatedNode != null) {
                    info.put("t", translatedNode.asText());
                    info.put("s", "T"); // translated key
                } else {
                    info.put("s", "U"); // untranslated key
                }
                //check if key added on current level
                if (parentTranslatedNode == null && parentOriginNode == null) {
                    info.put("s", "A"); // added key
                }
                ((ObjectNode) fullTranslation).set(fieldName, info);
            }
        }
    }

    private JsonNode getTranslatedOnly(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode resourceTranslation) {
        JsonNode customTranslation = getMergedCustomTranslation(tenantId, customerId, localeCode);
        if (resourceTranslation != null) {
            return merge(resourceTranslation, customTranslation);
        } else {
            return customTranslation;
        }
    }

    private JsonNode getParentTranslatedOnly(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode resourceTranslation) {
        JsonNode parentTranslation;
        if (customerId != null && !customerId.isNullUid()) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer.isSubCustomer()) {
                parentTranslation = getTranslatedOnly(tenantId, customer.getParentCustomerId(), localeCode, resourceTranslation);
            } else {
                parentTranslation = getTranslatedOnly(tenantId, null, localeCode, resourceTranslation);
            }
        } else {
            if (tenantId.isSysTenantId()) {
                parentTranslation = resourceTranslation;
            } else {
                parentTranslation = getTranslatedOnly(TenantId.SYS_TENANT_ID, null, localeCode, resourceTranslation);
            }
        }
        return parentTranslation;
    }

    private void addNonExisting(JsonNode mainNode, JsonNode newNode) {
        Iterator<String> fieldNames = newNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            if (jsonNode != null) {
                if (jsonNode.isObject()) {
                    addNonExisting(jsonNode, newNode.get(fieldName));
                } else if (jsonNode.isArray()) {
                    for (int i = 0; i < jsonNode.size(); i++) {
                        addNonExisting(jsonNode.get(i), newNode.get(fieldName).get(i));
                    }
                }
            } else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = newNode.get(fieldName);
                    if (value.isNull()) {
                        continue;
                    }
                    ((ObjectNode) mainNode).set(fieldName, value);
                }
            }
        }
    }

}
