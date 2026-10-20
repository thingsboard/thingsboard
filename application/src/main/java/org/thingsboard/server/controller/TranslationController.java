// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.Hashing;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.translation.TbCustomTranslationService;
import org.thingsboard.server.service.translation.TranslationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.thingsboard.common.util.JacksonUtil.writeValueAsStringWithDefaultPrettyPrinter;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class TranslationController extends BaseController {

    public static final List<String> LOCALE_CODES_TO_EXCLUDE = Arrays.asList("ru_UA", "ccp_IN", "ccp_BD", "ii_CN", "sat_IN");

    private static final String CUSTOM_TRANSLATION_INFO_EXAMPLE = "\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "[\n" +
            "  {\n" +
            "    \"localeCode\": \"uk_UA\",\n" +
            "    \"language\": \"Ukrainian (українська)\",\n" +
            "    \"country\": \"Україна\",\n" +
            "    \"progress\": 32\n" +
            "  },\n" +
            "  {\n" +
            "    \"localeCode\": \"es_ES\",\n" +
            "    \"language\": \"Spanish (español)\",\n" +
            "    \"country\": \"España\",\n" +
            "    \"progress\": 79\n" +
            "  }" +
            "]" +
            MARKDOWN_CODE_BLOCK_END;

    private static final ObjectNode AVAILABLE_JAVA_LOCALE_CODES = JacksonUtil.newObjectNode();

    static {
        List<Locale> availableLocales = Arrays.stream(DateFormat.getAvailableLocales())
                .filter(availableLocale -> StringUtils.countMatches(availableLocale.toString(), "_") == 1
                        && !LOCALE_CODES_TO_EXCLUDE.contains(availableLocale.toString())
                        && !availableLocale.toString().matches(".*\\d+.*"))
                .toList();
        for (Locale availableLocale : availableLocales) {
            String displayLanguage = availableLocale.getDisplayLanguage(availableLocale);
            String displayCountry = availableLocale.getDisplayCountry(availableLocale).isBlank() ? "" : " (" + availableLocale.getDisplayCountry(availableLocale) + ")";
            AVAILABLE_JAVA_LOCALE_CODES.put(availableLocale.toString(), displayLanguage + displayCountry);
        }
    }

    private final TbCustomTranslationService tbCustomTranslationService;
    private final TranslationService translationService;
    private final WhiteLabelingService whiteLabelingService;

    @ApiOperation(value = "Get Translation info (getTranslationInfos)",
            notes = "Fetch the list of customized locales and corresponding details such as language display name," +
                    " country display name and translation progress percentage." +
                    "\n\n Response example: " + CUSTOM_TRANSLATION_INFO_EXAMPLE +
                    ControllerConstants.WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/info")
    public List<TranslationInfo> getTranslationInfos() throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        return translationService.getTranslationInfos(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
    }

    @ApiOperation(value = "Get list of available locales (getAvailableLocales)",
            notes = "Fetch the list of customized locales from all levels" + ControllerConstants.WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/availableLocales")
    public JsonNode getAvailableLocales() throws ThingsboardException {
        ObjectNode result = JacksonUtil.newObjectNode();
        Set<String> availableLocaleCodes = translationService.getAvailableLocaleCodes(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
        for (String localeCode : availableLocaleCodes) {
            String[] locale_parts = localeCode.split("_");
            Locale locale = new Locale(locale_parts[0], locale_parts[1]);
            String displayLanguage = locale.getDisplayLanguage(locale);
            String displayCountry = locale.getDisplayCountry(locale).isBlank() ? "" : " (" + locale.getDisplayCountry(locale) + ")";
            result.put(localeCode, displayLanguage + displayCountry);
        }
        return result;
    }

    @ApiOperation(value = "Get list of available java locales (getAvailableJavaLocales)",
            notes = "The result is map where key is locale code and value is locale language and country")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/availableJavaLocales")
    public JsonNode getAvailableJavaLocales() {
        return AVAILABLE_JAVA_LOCALE_CODES;
    }

    @ApiOperation(value = "Get system translation for login page",
            notes = "Fetch the end-user translation for specified locale.")
    @ApiResponse(responseCode = "200", description = "Translation for login page in specified locale.",
            content = @Content(schema = @Schema(implementation = JsonNode.class)))
    @GetMapping(value = "/noauth/translation/login/{localeCode}")
    public void getLoginPageTranslation(
            @Parameter(description = "Locale code (e.g. 'en_US').")
            @PathVariable("localeCode") String localeCode,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        String domainName = request.getServerName();
        WhiteLabeling loginWL = whiteLabelingService.findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.LOGIN);
        TranslationCacheKey cacheKey;
        if (loginWL != null) {
            cacheKey = TranslationCacheKey.forLoginTranslation(loginWL.getTenantId(), loginWL.getCustomerId(), localeCode, domainName);
        } else {
            cacheKey = TranslationCacheKey.forLoginTranslation(localeCode, domainName);
        }
        if (StringUtils.isNotEmpty(etag) && StringUtils.remove(etag, '\"').equals(tbCustomTranslationService.getETag(cacheKey))) {
            response.setStatus(HttpStatus.NOT_MODIFIED.value());
        } else {
            JsonNode loginTranslation = translationService.getLoginTranslation(localeCode, domainName);
            String calculatedEtag = calculateTranslationEtag(loginTranslation);
            tbCustomTranslationService.putETag(cacheKey, calculatedEtag);
            response.setHeader("Etag", calculatedEtag);
            response.setContentType(APPLICATION_JSON_VALUE);
            compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(loginTranslation));
        }
    }

    @ApiOperation(value = "Get end-user all-to-one translation (getFullTranslation)",
            notes = "Fetch the end-user translation for specified locale. The result is the merge of user custom translation, " +
                    "system language translation and default locale translation.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = JsonNode.class)))
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/full/{localeCode}")
    public void getFullTranslation(
            @Parameter(description = "Locale code (e.g. 'en_US').")
            @PathVariable("localeCode") String localeCode,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
            @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader,
            HttpServletResponse response) throws Exception {
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();

        TranslationCacheKey cacheKey = TranslationCacheKey.forFullTranslation(tenantId, customerId, localeCode);
        if (StringUtils.isNotEmpty(etag) && StringUtils.remove(etag, '\"').equals(tbCustomTranslationService.getETag(cacheKey))) {
            response.setStatus(HttpStatus.NOT_MODIFIED.value());
        } else {
            JsonNode fullTranslation = translationService.getFullTranslation(tenantId, customerId, localeCode);
            String calculatedEtag = calculateTranslationEtag(fullTranslation);
            tbCustomTranslationService.putETag(cacheKey, calculatedEtag);
            response.setHeader("Etag", calculatedEtag);
            response.setContentType(APPLICATION_JSON_VALUE);
            compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, JacksonUtil.writeValueAsBytes(fullTranslation));
        }
    }

    @ApiOperation(value = "Get end-user multi-translation for basic edit (getTranslationForBasicEdit)",
            notes = "Fetch the translation info map where value is info object containing key translation, origin translation, " +
                    "translation of parent level, translation status.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/edit/basic/{localeCode}", produces = APPLICATION_JSON_VALUE)
    public JsonNode getTranslationForBasicEdit(@PathVariable("localeCode") String localeCode) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();

        return translationService.getTranslationForBasicEdit(tenantId, customerId, localeCode);
    }

    @ApiOperation(value = "Download end-user all-to-one translation (downloadFullTranslation)",
            notes = "Fetch the end-user translation for the specified locale. The result is a json file with merged user custom translation, " +
                    "system language translation and default locale translation.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/full/{localeCode}/download")
    public ResponseEntity<ByteArrayResource> downloadFullTranslation(@Parameter(description = "Locale code (e.g. 'en_US').")
                                                                     @PathVariable("localeCode") String localeCode) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();

        JsonNode fullSystemTranslation = checkNotNull(translationService.getFullTranslation(tenantId, customerId, localeCode));

        String fileName = localeCode + "_custom_translation.json";
        ByteArrayResource translation = new ByteArrayResource(writeValueAsStringWithDefaultPrettyPrinter(fullSystemTranslation).getBytes());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .header("x-filename", fileName)
                .contentLength(translation.contentLength())
                .header("Content-Type", MediaType.APPLICATION_JSON.toString())
                .body(translation);
    }

    protected String calculateTranslationEtag(JsonNode translation) {
        return Hashing.sha256().hashString(translation.toString(), StandardCharsets.UTF_8).toString();
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
