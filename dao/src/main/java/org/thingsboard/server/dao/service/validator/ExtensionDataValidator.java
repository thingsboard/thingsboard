// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.Quantity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.data.extension.RegistryAuth;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.extension.ExtensionDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtensionDataValidator extends DataValidator<Extension> {

    // Reserved environment-variable prefix injected by the platform.
    static final String RESERVED_ENV_PREFIX = "TB_EXT_";
    // Non-prefixed built-in env keys the platform injects (see ExtensionEnv). Reserved so user env
    // cannot shadow them on substrates where a duplicate key is last-occurrence-wins (Docker).
    static final Set<String> RESERVED_ENV_KEYS = Set.of("THINGSBOARD_URL");

    static final int MAX_ENV_ENTRIES = 100;
    // A Kubernetes Secret data key may not exceed 253 characters, and env is rendered into one Secret.
    // Accepting a longer key here would produce a spec that saves with 200 and can never be deployed.
    static final int MAX_ENV_KEY_LENGTH = 253;
    static final int MAX_ENV_VALUE_LENGTH = 32 * 1024;
    // Aggregate ceiling on the whole env map. Without it MAX_ENV_ENTRIES x MAX_ENV_VALUE_LENGTH accepts
    // ~3.2 MB, while the Kubernetes Secret that carries env is capped at 1 MiB by the API server: the save
    // would return 200 and the reconcile would fail asynchronously with a non-retryable 422, leaving the
    // extension FAILED and re-failing on every resync. Kept comfortably under the 1 MiB hard limit.
    static final int MAX_ENV_TOTAL_LENGTH = 256 * 1024;
    static final int MAX_ADDITIONAL_INFO_LENGTH = 64 * 1024;
    static final int MAX_IMAGE_LENGTH = 512;

    private static final Pattern TAG_PATTERN = Pattern.compile("^[\\w][\\w.-]{0,127}$");
    // A ThingsBoard Secret reference: ${secret:name;type:TYPE}. Registry passwords must use this form
    // so no plaintext credential is ever stored on the extension. This is the anchored, full-string
    // validation form of the canonical placeholder shape defined (unanchored, with capture groups for
    // resolution) by DefaultSecretConfigurationService.SECRET_PATTERN; keep the body in sync with it.
    private static final Pattern SECRET_REF_PATTERN = Pattern.compile("^\\$\\{secret:[^;{}]+;type:[^;{}]+}$");
    // Docker image reference WITHOUT tag/digest: optional registry host[:port] then lowercase repo path.
    private static final Pattern IMAGE_REFERENCE_PATTERN = Pattern.compile(
            "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*(?::[0-9]+)?/)?"
                    + "[a-z0-9]+(?:(?:[._]|__|[-]+)[a-z0-9]+)*(?:/[a-z0-9]+(?:(?:[._]|__|[-]+)[a-z0-9]+)*)*$");
    // Environment variable name: a C identifier (also a legal ConfigMap/Secret key).
    private static final Pattern ENV_KEY_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final TenantService tenantService;
    private final ExtensionDao extensionDao;

    // Field-injected so the unit-test constructor (tenantService, extensionDao) keeps these defaults.
    @Value("${extensions.image.allowed-registries:}")
    private String allowedRegistries = "";
    @Value("${extensions.k8s.resources.max-cpu:4}")
    private String maxCpu = "4";
    @Value("${extensions.k8s.resources.max-memory:8Gi}")
    private String maxMemory = "8Gi";
    @Value("${extensions.mode:disabled}")
    private String extensionsMode = "disabled";

    // The registry allowlist is the ONLY control over WHAT code runs on shared infrastructure, and it is
    // the one control in the extensions.image block that fails open when unset. That is a defensible
    // default for a single-tenant install; it is not one for public-cloud, which exists precisely to run
    // untrusted tenant code. Warn loudly at startup so an operator is not relying on a control that is off.
    @PostConstruct
    void warnOnOpenRegistryAllowlist() {
        if (!csv(allowedRegistries).isEmpty()) {
            return;
        }
        ExtensionMode mode;
        try {
            mode = ExtensionMode.fromKey(extensionsMode);
        } catch (IllegalArgumentException e) {
            return; // an invalid mode fails bean activation elsewhere; nothing to say here
        }
        if (mode == ExtensionMode.DISABLED) {
            return;
        }
        log.warn("extensions.image.allowed-registries is empty: any tenant may run ANY image from ANY registry"
                + " on this installation (extensions.mode={}). Set a trusted registry/repository prefix list"
                + " before allowing untrusted tenants to create extensions.", mode.getKey());
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Extension extension) {
        if (extension.getId() != null && extension.getId().isNullUid()) {
            throw new DataValidationException("Extension UUID must not be the reserved null value!");
        }
        if (extension.getTenantId() == null || extension.getTenantId().getId() == null) {
            throw new DataValidationException("Extension should be assigned to a tenant!");
        }
        if (extension.getTenantId().isSysTenantId()) {
            throw new DataValidationException("Extension cannot be assigned to the system tenant!");
        }
        if (!tenantService.tenantExists(extension.getTenantId())) {
            throw new DataValidationException("Extension references a non-existent tenant!");
        }
        validateString("Extension name", extension.getName());

        ExtensionContainerSpec spec = extension.getContainerSpec();
        if (spec == null) {
            throw new DataValidationException("Extension containerSpec is required!");
        }
        validateImage(spec);
        if (StringUtils.isBlank(spec.getTag())) {
            throw new DataValidationException("Extension containerSpec.tag is required!");
        }
        if (!TAG_PATTERN.matcher(spec.getTag()).matches()) {
            throw new DataValidationException("Extension containerSpec.tag is invalid!");
        }
        if (spec.getReplicas() < 1 || spec.getReplicas() > 20) {
            throw new DataValidationException("Extension containerSpec.replicas must be between 1 and 20!");
        }
        if (spec.getPort() < 1 || spec.getPort() > 65535) {
            throw new DataValidationException("Extension containerSpec.port must be between 1 and 65535!");
        }
        validateRegistryAuth(spec);
        validateEnv(spec);
        validateResources(spec.getResources());
        validateAdditionalInfo(extension);
    }

    private void validateImage(ExtensionContainerSpec spec) {
        if (StringUtils.isBlank(spec.getImage())) {
            throw new DataValidationException("Extension containerSpec.image is required!");
        }
        if (spec.getImage().length() > MAX_IMAGE_LENGTH) {
            throw new DataValidationException("Extension containerSpec.image is too long (max " + MAX_IMAGE_LENGTH + ")!");
        }
        if (containsWhitespace(spec.getImage())) {
            throw new DataValidationException("Extension containerSpec.image must not contain whitespace!");
        }
        // Reject a trailing ':tag' but allow a registry-host port; the tag separator follows the last '/'.
        String lastSegment = spec.getImage().substring(spec.getImage().lastIndexOf('/') + 1);
        if (lastSegment.indexOf(':') >= 0) {
            throw new DataValidationException("Extension containerSpec.image must not contain a tag (':'); set the tag separately!");
        }
        if (!IMAGE_REFERENCE_PATTERN.matcher(spec.getImage()).matches()) {
            throw new DataValidationException("Extension containerSpec.image is not a valid image reference!");
        }
        List<String> registries = csv(allowedRegistries);
        if (!registries.isEmpty() && registries.stream().noneMatch(r -> imageMatchesRegistry(spec.getImage(), r))) {
            throw new DataValidationException("Extension containerSpec.image must come from an allowed registry!");
        }
    }

    // A raw startsWith lets 'registry.example.com.evil.io/x' pass an allowlist of 'registry.example.com'.
    // Require the entry to match a whole path prefix: the image must equal it, or continue with a '/'.
    private static boolean imageMatchesRegistry(String image, String registry) {
        String prefix = registry.endsWith("/") ? registry : registry + "/";
        return image.equals(registry) || image.startsWith(prefix);
    }

    private void validateRegistryAuth(ExtensionContainerSpec spec) {
        RegistryAuth auth = spec.getRegistryAuth();
        if (auth == null) {
            return;
        }
        if (StringUtils.isBlank(auth.getUsername())) {
            throw new DataValidationException("Extension containerSpec.registryAuth.username is required!");
        }
        if (auth.getRegistry() != null && auth.getRegistry().length() > 253) {
            throw new DataValidationException("Extension containerSpec.registryAuth.registry is too long (max 253)!");
        }
        // No plaintext: the password must reference a ThingsBoard Secret, resolved at deploy time.
        if (StringUtils.isBlank(auth.getPassword()) || !SECRET_REF_PATTERN.matcher(auth.getPassword().trim()).matches()) {
            throw new DataValidationException("Extension containerSpec.registryAuth.password must be a ThingsBoard "
                    + "Secret reference, e.g. ${secret:acme-registry;type:TEXT}!");
        }
    }

    private void validateEnv(ExtensionContainerSpec spec) {
        Map<String, String> env = spec.getEnv();
        if (env == null) {
            return;
        }
        if (env.size() > MAX_ENV_ENTRIES) {
            throw new DataValidationException("Extension containerSpec.env must not exceed " + MAX_ENV_ENTRIES + " entries!");
        }
        long totalLength = 0;
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isEmpty(key)) {
                throw new DataValidationException("Extension containerSpec.env keys must not be blank!");
            }
            if (key.startsWith(RESERVED_ENV_PREFIX)) {
                throw new DataValidationException("Environment keys starting with " + RESERVED_ENV_PREFIX + " are reserved by the platform!");
            }
            if (RESERVED_ENV_KEYS.contains(key)) {
                throw new DataValidationException("Environment key '" + key + "' is reserved by the platform!");
            }
            if (key.length() > MAX_ENV_KEY_LENGTH) {
                throw new DataValidationException("Extension containerSpec.env key is too long (max " + MAX_ENV_KEY_LENGTH + ")!");
            }
            if (!ENV_KEY_PATTERN.matcher(key).matches()) {
                throw new DataValidationException("Extension containerSpec.env key '" + key
                        + "' is not a valid environment variable name (letters, digits, underscore; not starting with a digit)!");
            }
            String value = entry.getValue();
            if (value != null && value.length() > MAX_ENV_VALUE_LENGTH) {
                throw new DataValidationException("Extension containerSpec.env value for '" + key + "' is too long (max " + MAX_ENV_VALUE_LENGTH + ")!");
            }
            totalLength += key.length() + (value != null ? value.length() : 0);
        }
        if (totalLength > MAX_ENV_TOTAL_LENGTH) {
            throw new DataValidationException("Extension containerSpec.env is too large (" + totalLength
                    + " characters, max " + MAX_ENV_TOTAL_LENGTH + " in total)!");
        }
    }

    private void validateResources(ResourceSpec resources) {
        // An omitted `resources` is not "no resources": every consumer substitutes new ResourceSpec(),
        // which resolves to the default tier. Validate that same substitute, or a request without the
        // field would slip past a ceiling that rejects the identical request with the size spelled out.
        if (resources == null) {
            resources = new ResourceSpec();
        }
        BigDecimal maxCpuVal = parseCeiling(maxCpu);
        BigDecimal maxMemVal = parseCeiling(maxMemory);
        // A size resolves to a curated cpu/memory pair (request <= limit by construction), so the only
        // thing left to enforce is the platform ceiling: an operator who lowers max-cpu/max-memory
        // thereby restricts which sizes this instance accepts.
        checkCeiling(resources.effectiveCpuRequest(), "cpuRequest", maxCpuVal);
        checkCeiling(resources.effectiveCpuLimit(), "cpuLimit", maxCpuVal);
        checkCeiling(resources.effectiveMemoryRequest(), "memoryRequest", maxMemVal);
        checkCeiling(resources.effectiveMemoryLimit(), "memoryLimit", maxMemVal);
    }

    private void validateAdditionalInfo(Extension extension) {
        JsonNode additionalInfo = extension.getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.toString().length() > MAX_ADDITIONAL_INFO_LENGTH) {
            throw new DataValidationException("Extension additionalInfo is too large (max " + MAX_ADDITIONAL_INFO_LENGTH + " chars)!");
        }
    }

    // Rejects a resolved (size-derived) quantity that exceeds the configured platform ceiling.
    private static void checkCeiling(String quantity, String field, BigDecimal max) {
        if (max == null) {
            return;
        }
        BigDecimal value = parseQuantity(quantity);
        if (value.compareTo(max) > 0) {
            throw new DataValidationException("Extension containerSpec.resources." + field + " exceeds the configured maximum!");
        }
    }

    private static BigDecimal parseCeiling(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return parseQuantity(raw);
        } catch (RuntimeException e) {
            return null; // a misconfigured ceiling must not block saves; it just disables the bound.
        }
    }

    // Parses a Kubernetes quantity into a base value (CPU in cores, memory in bytes).
    static BigDecimal parseQuantity(String raw) {
        return Quantity.getAmountInBytes(new Quantity(raw.trim()));
    }

    private static List<String> csv(String raw) {
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    @Override
    protected Extension validateUpdate(TenantId tenantId, Extension extension) {
        Extension existing = extensionDao.findByTenantIdAndId(tenantId, extension.getId());
        if (existing == null) {
            throw new DataValidationException("Cannot update non-existent extension!");
        }
        if (extension.getSlug() != null && !existing.getSlug().equals(extension.getSlug())) {
            throw new DataValidationException("Extension slug is immutable!");
        }
        return existing;
    }

    private static boolean containsWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

}
