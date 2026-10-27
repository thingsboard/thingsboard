// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionInfo;
import org.thingsboard.server.common.data.extension.ExtensionResourceSizeInfo;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ExtensionStatus;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.extension.spi.DeploymentHealth;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level test of the extension CRUD + lifecycle surface. Runs in public-cloud mode (so the slug
 * keeps its random suffix) with the reconciler mocked to report no live deployment — lifecycle calls
 * only change the persisted state and /status reflects PROVISIONING. The gateway-auth target
 * /api/ext-auth is covered separately by {@link ExtensionControllerAuthTest}.
 */
@DaoSqlTest
@TestPropertySource(locations = {"classpath:application-test.properties", "classpath:sql-test.properties"},
        properties = {"extensions.mode=public-cloud"})
public class ExtensionControllerTest extends AbstractControllerTest {

    // public-cloud loads the real Kubernetes orchestration beans; replace the reconciler with a mock
    // that reports no live deployment so these HTTP CRUD/lifecycle tests never reach a real cluster.
    @MockBean
    private ExtensionReconciler extensionReconciler;

    @Before
    public void stubReconciler() {
        when(extensionReconciler.describe(any())).thenReturn(DeploymentHealth.missing());
        when(extensionReconciler.listManagedSlugs()).thenReturn(Set.of());
    }

    // The default profile ships with room for a single SMALL extension. Only the quota tests below care
    // about limits, so every other test starts from an unlimited profile.
    @Before
    public void liftExtensionQuota() throws Exception {
        setDefaultProfileExtensionLimits(0, 0, 0);
    }

    /* --- Save API --- */

    @Test
    public void saveExtension_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        loginSysAdmin();

        doPost("/api/extension", validExtension("Test extension"))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void saveExtension_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        loginCustomerUser();

        doPost("/api/extension", validExtension("Test extension"))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    @Test
    public void saveExtension_whenCreatingAsTenantAdmin_shouldReturnAcceptedWithAllocatedSlug() throws Exception {
        loginTenantAdmin();

        Extension saved = createExtension("Test extension");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId().getEntityType()).isEqualTo(EntityType.EXTENSION);
        assertThat(saved.getCreatedTime()).isPositive();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getName()).isEqualTo("Test extension");
        assertThat(saved.getSlug()).matches("test-extension-[0-9a-f]{4}");
        assertThat(saved.getState()).isEqualTo(ExtensionState.PROVISIONING);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getExternalId()).isNull();
    }

    @Test
    public void saveExtension_whenSlugBaseIsSupplied_shouldUseItAndNeverReturnIt() throws Exception {
        loginTenantAdmin();

        // slugBase is WRITE_ONLY, so serializing an Extension object drops it — send raw JSON like a real client
        ObjectNode body = (ObjectNode) JacksonUtil.valueToTree(validExtension("Custom base"));
        body.put("slugBase", "acme-billing");

        Extension saved = readResponse(
                doPost("/api/extension", body).andExpect(status().isAccepted()), Extension.class);

        assertThat(saved.getSlug()).matches("acme-billing-[0-9a-f]{4}");
        // slugBase is write-only: consumed for allocation, never stored or returned
        assertThat(saved.getSlugBase()).isNull();
    }

    @Test
    public void saveExtension_whenNameIsTaken_shouldReturnBadRequest() throws Exception {
        loginTenantAdmin();
        createExtension("Duplicate name");

        doPost("/api/extension", validExtension("Duplicate name"))
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(equalTo("Extension with such name already exists!")));
    }

    @Test
    public void saveExtension_whenEnvKeyUsesReservedPrefix_shouldReturnBadRequest() throws Exception {
        loginTenantAdmin();

        Extension extension = validExtension("Reserved env");
        extension.getContainerSpec().setEnv(Map.of("TB_EXT_SLUG", "spoofed"));

        doPost("/api/extension", extension)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("reserved by the platform")));
    }

    @Test
    public void saveExtension_whenUpdatingAsTenantAdmin_shouldKeepSlugAndState() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Before update");

        created.setName("After update");
        created.getContainerSpec().setTag("2.0.0");
        created.getContainerSpec().setEnv(Map.of("DB_PASSWORD", "updated-secret"));

        Extension updated = readResponse(
                doPost("/api/extension", created).andExpect(status().isAccepted()), Extension.class);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("After update");
        assertThat(updated.getSlug()).isEqualTo(created.getSlug());
        assertThat(updated.getState()).isEqualTo(created.getState());
        assertThat(updated.getCreatedTime()).isEqualTo(created.getCreatedTime());
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getContainerSpec().getTag()).isEqualTo("2.0.0");
        assertThat(updated.getContainerSpec().getEnv()).containsEntry("DB_PASSWORD", "updated-secret");
    }

    @Test
    public void saveExtension_whenChangingSlug_shouldReturnBadRequest() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Slug immutable");

        created.setSlug("hijacked-slug-0000");

        doPost("/api/extension", created)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(equalTo("Extension slug is immutable!")));
    }

    /* --- Read API --- */

    @Test
    public void getExtensionById_whenOwningTenant_shouldReturnFullEntityIncludingEnv() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Detail read");

        Extension found = doGet("/api/extension/" + created.getUuidId(), Extension.class);

        assertThat(found.getId()).isEqualTo(created.getId());
        // the detail read returns env so a GET-modify-PUT edit can round-trip the spec
        assertThat(found.getContainerSpec().getEnv()).containsEntry("DB_PASSWORD", "secret");
    }

    @Test
    public void getExtensionById_whenDifferentTenant_shouldReturnNotFound() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Cross tenant");

        loginDifferentTenant();
        doGet("/api/extension/" + created.getUuidId()).andExpect(status().isNotFound());
    }

    @Test
    public void getExtensions_shouldReturnLightProjectionWithoutEnv() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("List projection");

        PageData<ExtensionInfo> page = doGetTyped(
                "/api/tenant/extensions?pageSize=10&page=0", new TypeReference<PageData<ExtensionInfo>>() {});

        assertThat(page.getData()).hasSize(1);
        ExtensionInfo info = page.getData().get(0);
        assertThat(info.getId()).isEqualTo(created.getId());
        assertThat(info.getContainerSpec().getImage()).isEqualTo("registry.example.com/acme/billing-ext");
        // the list projection strips env (may contain secrets) and resources
        assertThat(info.getContainerSpec().getEnv()).isNull();
        assertThat(info.getContainerSpec().getResources()).isNull();
        // and is enriched with the data-plane URL for the table view
        assertThat(info.getUrl()).isEqualTo("/api/extension/route/" + created.getSlug());
    }

    @Test
    public void getExtensions_whenSorting_shouldSupportAllDocumentedProperties() throws Exception {
        loginTenantAdmin();
        createExtension("Sort A");
        createExtension("Sort B");

        // regression: JPQL paging used to 500 on mapped sort properties
        for (String property : new String[]{"createdTime", "name", "state"}) {
            PageData<ExtensionInfo> page = doGetTyped(
                    "/api/tenant/extensions?pageSize=10&page=0&sortProperty=" + property + "&sortOrder=ASC",
                    new TypeReference<PageData<ExtensionInfo>>() {});
            assertThat(page.getData()).hasSize(2);
        }

        doGet("/api/tenant/extensions?pageSize=10&page=0&sortProperty=slug")
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getExtensionStatus_whenDeploymentNotObserved_shouldReportProvisioning() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Status read");

        ExtensionStatus status = doGet("/api/extension/" + created.getUuidId() + "/status", ExtensionStatus.class);

        assertThat(status.getExtensionId()).isEqualTo(created.getId());
        assertThat(status.getSlug()).isEqualTo(created.getSlug());
        assertThat(status.getState()).isEqualTo(ExtensionState.PROVISIONING);
        assertThat(status.getUrl()).isEqualTo("/api/extension/route/" + created.getSlug());
    }

    @Test
    public void getExtensionLogs_whenLinesOutOfRange_shouldReturnBadRequest() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Logs lines");

        doGet("/api/extension/" + created.getUuidId() + "/logs?lines=0")
                .andExpect(status().isBadRequest());
    }

    // Reading logs is the one read path a user can drive in a loop — the UI dialog polls it for as long
    // as it is open, and each call reaches the orchestrator. Without a limit a script can turn that into
    // sustained API-server load, so the endpoint has its own per-user budget on top of the generic REST
    // limits. Uses the shipped default (20 per 60 s); this exhausts the calling user's bucket, which is
    // fine because no other case here reaches the endpoint past its parameter check.
    @Test
    public void getExtensionLogs_whenCalledInALoop_shouldRateLimitPerUser() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Logs rate limit");
        String url = "/api/extension/" + created.getUuidId() + "/logs";

        for (int i = 0; i < 20; i++) {
            doGet(url).andExpect(status().isOk());
        }

        doGet(url).andExpect(status().isTooManyRequests());
    }

    /* --- Lifecycle API --- */

    @Test
    public void stopAndStartExtension_shouldFlipPersistedState() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Stop start");

        doPost("/api/extension/" + created.getUuidId() + "/stop")
                .andExpect(status().isAccepted());
        assertThat(doGet("/api/extension/" + created.getUuidId(), Extension.class).getState())
                .isEqualTo(ExtensionState.STOPPED);

        doPost("/api/extension/" + created.getUuidId() + "/start")
                .andExpect(status().isAccepted());
        assertThat(doGet("/api/extension/" + created.getUuidId(), Extension.class).getState())
                .isEqualTo(ExtensionState.PROVISIONING);
    }

    @Test
    public void startExtension_whenNotStopped_shouldReturnBadRequest() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Start invalid");

        doPost("/api/extension/" + created.getUuidId() + "/start")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("can only be started from the STOPPED state")));
    }

    @Test
    public void restartExtension_shouldReturnAcceptedAndKeepState() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Restart");

        doPost("/api/extension/" + created.getUuidId() + "/restart")
                .andExpect(status().isAccepted());
        assertThat(doGet("/api/extension/" + created.getUuidId(), Extension.class).getState())
                .isEqualTo(ExtensionState.PROVISIONING);
    }

    @Test
    public void lifecycleEndpoints_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Lifecycle forbidden");

        loginCustomerUser();
        for (String action : new String[]{"stop", "start", "restart"}) {
            doPost("/api/extension/" + created.getUuidId() + "/" + action)
                    .andExpect(status().isForbidden())
                    .andExpect(statusReason(equalTo(msgErrorPermission)));
        }
    }

    /* --- Delete API --- */

    @Test
    public void deleteExtension_whenOwningTenant_shouldReturnAcceptedAndRemoveEntity() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("To delete");

        doDelete("/api/extension/" + created.getUuidId())
                .andExpect(status().isAccepted());

        doGet("/api/extension/" + created.getUuidId()).andExpect(status().isNotFound());
    }

    @Test
    public void deleteExtension_whenUserIsCustomerUser_shouldReturnForbidden() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Delete forbidden");

        loginCustomerUser();
        doDelete("/api/extension/" + created.getUuidId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));
    }

    /* --- Resource sizes --- */

    @Test
    public void getResourceSizes_shouldReturnCatalogWithDefaultFlag() throws Exception {
        loginTenantAdmin();

        List<ExtensionResourceSizeInfo> sizes = doGetTyped(
                "/api/extension/resourceSizes", new TypeReference<List<ExtensionResourceSizeInfo>>() {});

        assertThat(sizes).hasSize(ResourceSize.values().length);
        assertThat(sizes).extracting(ExtensionResourceSizeInfo::name)
                .contains(ResourceSize.XSMALL, ResourceSize.SMALL, ResourceSize.MEDIUM, ResourceSize.XXLARGE);
        // exactly one tier is flagged as the default, and it is the platform default
        assertThat(sizes).filteredOn(ExtensionResourceSizeInfo::isDefault)
                .extracting(ExtensionResourceSizeInfo::name)
                .containsExactly(ResourceSize.DEFAULT);
        ExtensionResourceSizeInfo medium = sizes.stream()
                .filter(s -> s.name() == ResourceSize.MEDIUM).findFirst().orElseThrow();
        assertThat(medium.cpuRequest()).isEqualTo("500m");
        assertThat(medium.cpuLimit()).isEqualTo("1");
        // memory request and limit are pinned equal
        assertThat(medium.memoryRequest()).isEqualTo("512Mi");
        assertThat(medium.memoryLimit()).isEqualTo("512Mi");
    }

    @Test
    public void getResourceSizes_whenUserIsSysAdmin_shouldReturnForbidden() throws Exception {
        loginSysAdmin();
        doGet("/api/extension/resourceSizes").andExpect(status().isForbidden());
    }

    @Test
    public void saveExtension_withResourceSize_shouldRoundTrip() throws Exception {
        loginTenantAdmin();
        Extension ext = validExtension("Sized extension");
        ResourceSpec res = new ResourceSpec();
        res.setSize(ResourceSize.LARGE);
        ext.getContainerSpec().setResources(res);

        Extension created = readResponse(
                doPost("/api/extension", ext).andExpect(status().isAccepted()), Extension.class);

        Extension found = doGet("/api/extension/" + created.getUuidId(), Extension.class);
        assertThat(found.getContainerSpec().getResources().getSize()).isEqualTo(ResourceSize.LARGE);
    }

    /* --- Tenant profile quotas --- */
    // validExtension() carries no explicit size, so every pod uses the SMALL tier: 500m cpu limit / 256Mi.

    @Test
    public void saveExtension_whenTenantProfileMaxExtensionsReached_shouldReturnBadRequest() throws Exception {
        setDefaultProfileExtensionLimits(1, 0, 0);
        try {
            loginTenantAdmin();
            createExtension("Count quota one");
            doPost("/api/extension", validExtension("Count quota two"))
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(containsString("Extension limit reached")));
        } finally {
            setDefaultProfileExtensionLimits(0, 0, 0);
        }
    }

    @Test
    public void saveExtension_whenTenantProfileCpuQuotaExceeded_shouldReturnBadRequest() throws Exception {
        setDefaultProfileExtensionLimits(0, 1000, 0); // room for exactly two SMALL pods
        try {
            loginTenantAdmin();
            createExtension("Cpu quota one");
            createExtension("Cpu quota two");
            doPost("/api/extension", validExtension("Cpu quota three"))
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(containsString("CPU limit reached")));
        } finally {
            setDefaultProfileExtensionLimits(0, 0, 0);
        }
    }

    @Test
    public void saveExtension_whenTenantProfileMemoryQuotaExceeded_shouldReturnBadRequest() throws Exception {
        setDefaultProfileExtensionLimits(0, 0, 512); // room for exactly two SMALL pods
        try {
            loginTenantAdmin();
            createExtension("Mem quota one");
            createExtension("Mem quota two");
            doPost("/api/extension", validExtension("Mem quota three"))
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(containsString("memory limit reached")));
        } finally {
            setDefaultProfileExtensionLimits(0, 0, 0);
        }
    }

    @Test
    public void updateExtension_whenDownscaleAndOverQuota_shouldReturnAccepted() throws Exception {
        loginTenantAdmin();
        Extension created = createExtension("Downscale over quota");
        ResourceSpec large = new ResourceSpec();
        large.setSize(ResourceSize.XLARGE);
        created.getContainerSpec().setResources(large);
        created = readResponse(
                doPost("/api/extension", created).andExpect(status().isAccepted()), Extension.class);

        setDefaultProfileExtensionLimits(0, 1000, 0); // quota below the extension's current 4000m
        try {
            loginTenantAdmin();
            created.getContainerSpec().getResources().setSize(ResourceSize.LARGE); // 4000m -> 2000m
            doPost("/api/extension", created).andExpect(status().isAccepted());
        } finally {
            setDefaultProfileExtensionLimits(0, 0, 0);
        }
    }

    @Test
    public void startExtension_whenQuotaNoLongerFits_shouldReturnBadRequest() throws Exception {
        loginTenantAdmin();
        Extension stopped = createExtension("Start quota stopped");
        doPost("/api/extension/" + stopped.getUuidId() + "/stop").andExpect(status().isAccepted());
        createExtension("Start quota occupier"); // occupies 500m

        setDefaultProfileExtensionLimits(0, 500, 0); // the occupier alone fills the quota
        try {
            loginTenantAdmin();
            doPost("/api/extension/" + stopped.getUuidId() + "/start")
                    .andExpect(status().isBadRequest())
                    .andExpect(statusReason(containsString("CPU limit reached")));
        } finally {
            setDefaultProfileExtensionLimits(0, 0, 0);
        }
    }

    private void setDefaultProfileExtensionLimits(long maxExtensions, long cpuMillicores, long memoryMb) throws Exception {
        loginSysAdmin();
        EntityInfo profileInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        TenantProfile profile = doGet("/api/tenantProfile/" + profileInfo.getId().getId(), TenantProfile.class);
        DefaultTenantProfileConfiguration configuration =
                (DefaultTenantProfileConfiguration) profile.getProfileData().getConfiguration();
        configuration.setMaxExtensions(maxExtensions);
        configuration.setMaxExtensionCpuMillicores(cpuMillicores);
        configuration.setMaxExtensionMemoryMb(memoryMb);
        doPost("/api/tenantProfile", profile, TenantProfile.class);
    }

    /* --- helpers --- */

    private Extension createExtension(String name) throws Exception {
        return readResponse(
                doPost("/api/extension", validExtension(name)).andExpect(status().isAccepted()), Extension.class);
    }

    private static Extension validExtension(String name) {
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setImage("registry.example.com/acme/billing-ext");
        spec.setTag("1.0.0");
        spec.setEnv(Map.of("DB_PASSWORD", "secret"));

        Extension extension = new Extension();
        extension.setName(name);
        extension.setContainerSpec(spec);
        return extension;
    }

}
