// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.RegistryAuth;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.extension.ExtensionDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtensionDataValidatorTest {

    private TenantService tenantService;
    private ExtensionDao extensionDao;
    private ExtensionDataValidator validator;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        extensionDao = mock(ExtensionDao.class);
        validator = new ExtensionDataValidator(tenantService, extensionDao);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        when(tenantService.tenantExists(any())).thenReturn(true);
    }

    private Extension validExtension() {
        Extension ext = new Extension();
        ext.setTenantId(tenantId);
        ext.setName("Acme Billing");
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag("1.4.0");
        spec.setReplicas(2);
        spec.setPort(8090);
        ext.setContainerSpec(spec);
        return ext;
    }

    @Test
    void validExtensionPassesOnCreate() {
        assertThatCode(() -> validator.validate(validExtension(), Extension::getTenantId)).doesNotThrowAnyException();
    }

    @Test
    void rejectsReservedEnvKey() {
        Extension ext = validExtension();
        ext.getContainerSpec().setEnv(Map.of("TB_EXT_SLUG", "hack"));
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void rejectsReservedNonPrefixedEnvKey() {
        // THINGSBOARD_URL is a platform built-in but is not TB_EXT_-prefixed; it must still be rejected
        Extension ext = validExtension();
        ext.getContainerSpec().setEnv(Map.of("THINGSBOARD_URL", "http://attacker.example"));
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void rejectsImageWithTag() {
        Extension ext = validExtension();
        ext.getContainerSpec().setImage("registry.example.com/acme/billing-ext:1.4.0");
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void acceptsRegistryHostPortInImage() {
        Extension ext = validExtension();
        ext.getContainerSpec().setImage("registry.internal:5000/acme/billing-ext");
        assertThatCode(() -> validator.validate(ext, Extension::getTenantId)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRegistryAuthWithPlaintextPassword() {
        Extension ext = validExtension();
        RegistryAuth auth = new RegistryAuth();
        auth.setUsername("robot$acme");
        auth.setPassword("plaintext-not-a-secret-ref");
        ext.getContainerSpec().setRegistryAuth(auth);
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("registryAuth.password");
    }

    @Test
    void acceptsRegistryAuthWithSecretReference() {
        Extension ext = validExtension();
        RegistryAuth auth = new RegistryAuth();
        auth.setRegistry("registry.example.com");
        auth.setUsername("robot$acme");
        auth.setPassword("${secret:acme-registry;type:TEXT}");
        ext.getContainerSpec().setRegistryAuth(auth);
        assertThatCode(() -> validator.validate(ext, Extension::getTenantId)).doesNotThrowAnyException();
    }

    @Test
    void acceptsEveryResourceSizeUnderDefaultCeilings() {
        for (ResourceSize size : ResourceSize.values()) {
            Extension ext = validExtension();
            ResourceSpec res = new ResourceSpec();
            res.setSize(size);
            ext.getContainerSpec().setResources(res);
            assertThatCode(() -> validator.validate(ext, Extension::getTenantId))
                    .as("size %s should pass under default 4 CPU / 8Gi ceilings", size)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsSizeAboveLoweredCpuCeiling() throws Exception {
        setField("maxCpu", "1"); // lower the ceiling so larger tiers no longer fit
        Extension ext = validExtension();
        ResourceSpec res = new ResourceSpec();
        res.setSize(ResourceSize.XLARGE); // cpuRequest 2 exceeds the lowered 1-core ceiling
        ext.getContainerSpec().setResources(res);
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("cpuRequest");
    }

    @Test
    void nullResourcesPass() {
        Extension ext = validExtension();
        ext.getContainerSpec().setResources(null);
        assertThatCode(() -> validator.validate(ext, Extension::getTenantId)).doesNotThrowAnyException();
    }

    // An omitted `resources` resolves to the default tier at deploy time, so it must be held to the same
    // ceiling as the identical request with the size spelled out — otherwise it slips past validation and
    // fails asynchronously on the cluster instead.
    @Test
    void nullResourcesAreValidatedAsTheDefaultTier() throws Exception {
        setField("maxMemory", "128Mi"); // below the default tier's 256Mi
        Extension ext = validExtension();
        ext.getContainerSpec().setResources(null);
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("memory");
    }

    private void setField(String name, Object value) throws Exception {
        java.lang.reflect.Field field = ExtensionDataValidator.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(validator, value);
    }

    @Test
    void rejectsOverlongImage() {
        Extension ext = validExtension();
        ext.getContainerSpec().setImage("registry.example.com/" + "a".repeat(600));
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    // env is rendered into ONE Kubernetes Secret, capped at 1 MiB by the API server. Per-entry limits
    // alone accept ~3.2 MB, which saves with 200 and then fails the reconcile with a non-retryable 422.
    @Test
    void rejectsEnvWholeMapAboveTheAggregateBudget() {
        Extension ext = validExtension();
        Map<String, String> env = new HashMap<>();
        String value = "x".repeat(32 * 1024);
        for (int i = 0; i < 16; i++) {
            env.put("BIG_" + i, value);
        }
        ext.getContainerSpec().setEnv(env);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, ext))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("too large");
    }

    // A Secret data key may not exceed 253 characters; a longer one is accepted by no substrate.
    @Test
    void rejectsEnvKeyLongerThanTheSecretKeyLimit() {
        Extension ext = validExtension();
        ext.getContainerSpec().setEnv(Map.of("K".repeat(254), "v"));

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, ext))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void rejectsInvalidEnvKey() {
        Extension ext = validExtension();
        ext.getContainerSpec().setEnv(Map.of("foo bar", "x"));
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void rejectsImageWithDigest() {
        Extension ext = validExtension();
        ext.getContainerSpec().setImage("registry.example.com/acme/billing-ext@sha256:abcdef");
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void rejectsMissingTenant() {
        Extension ext = validExtension();
        when(tenantService.tenantExists(any())).thenReturn(false);
        assertThatThrownBy(() -> validator.validate(ext, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void rejectsSlugChangeOnUpdate() {
        Extension incoming = validExtension();
        ExtensionId id = new ExtensionId(UUID.randomUUID());
        incoming.setId(id);
        incoming.setSlug("acme-billing-bbbb");

        Extension existing = validExtension();
        existing.setId(id);
        existing.setSlug("acme-billing-aaaa");
        when(extensionDao.findByTenantIdAndId(tenantId, id)).thenReturn(existing);

        assertThatThrownBy(() -> validator.validate(incoming, Extension::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("immutable");
    }

    @Test
    void allowsUpdateWhenSlugOmitted() {
        Extension incoming = validExtension();
        ExtensionId id = new ExtensionId(UUID.randomUUID());
        incoming.setId(id);
        incoming.setSlug(null);

        Extension existing = validExtension();
        existing.setId(id);
        existing.setSlug("acme-billing-aaaa");
        when(extensionDao.findByTenantIdAndId(tenantId, id)).thenReturn(existing);

        assertThatCode(() -> validator.validate(incoming, Extension::getTenantId)).doesNotThrowAnyException();
    }

}
