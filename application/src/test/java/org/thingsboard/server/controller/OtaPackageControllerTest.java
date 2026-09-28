// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@DaoSqlTest
public class OtaPackageControllerTest extends AbstractControllerTest {

    private IdComparator<OtaPackageInfo> idComparator = new IdComparator<>();

    public static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final String CHECKSUM_ALGORITHM = "SHA256";
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    private Tenant savedTenant;
    private User tenantAdmin;
    private DeviceProfileId deviceProfileId;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveFirmware() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        Mockito.reset(tbClusterService, auditLogService);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedFirmwareInfo, savedFirmwareInfo.getId(), savedFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundFirmwareInfo, foundFirmwareInfo.getId(), foundFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void saveOtaPackageInfoWithViolationOfLengthValidation() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(StringUtils.randomAlphabetic(300));
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);
        String msgError = msgErrorFieldLength("title");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));

        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("version");
        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo,
                savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));

        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(true);
        msgError = msgErrorFieldLength("url");
        firmwareInfo.setUrl(StringUtils.randomAlphabetic(300));
        doPost("/api/otaPackage", firmwareInfo)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        firmwareInfo.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(firmwareInfo, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveFirmwareData() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Assert.assertNotNull(savedFirmwareInfo);
        Assert.assertNotNull(savedFirmwareInfo.getId());
        Assert.assertTrue(savedFirmwareInfo.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedFirmwareInfo.getTenantId());
        Assert.assertEquals(firmwareInfo.getTitle(), savedFirmwareInfo.getTitle());
        Assert.assertEquals(firmwareInfo.getVersion(), savedFirmwareInfo.getVersion());

        savedFirmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        save(new SaveOtaPackageInfoRequest(savedFirmwareInfo, false));

        OtaPackageInfo foundFirmwareInfo = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertEquals(foundFirmwareInfo.getTitle(), savedFirmwareInfo.getTitle());

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        Mockito.reset(tbClusterService, auditLogService);

        OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString()
                + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        Assert.assertEquals(FILE_NAME, savedFirmware.getFileName());
        Assert.assertEquals(CONTENT_TYPE, savedFirmware.getContentType());
        Assert.assertEquals(CHECKSUM_ALGORITHM, savedFirmware.getChecksumAlgorithm().name());
        Assert.assertEquals(CHECKSUM, savedFirmware.getChecksum());

        testNotifyEntityEntityGroupNullAllOneTime(new OtaPackageInfo(savedFirmware), savedFirmware.getId(), savedFirmware.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testUpdateFirmwareFromDifferentTenant() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/otaPackage",
                new SaveOtaPackageInfoRequest(savedFirmwareInfo, false))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + "OTA_PACKAGE" + " '" + firmwareInfo.getTitle() + "'!")));

        testNotifyEntityNever(savedFirmwareInfo.getId(), savedFirmwareInfo);

        deleteDifferentTenant();
    }

    @Test
    public void testFindFirmwareInfoById() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        OtaPackageInfo foundFirmware = doGet("/api/otaPackage/info/" + savedFirmwareInfo.getId().getId().toString(), OtaPackageInfo.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmwareInfo, foundFirmware);
    }

    @Test
    public void testFindFirmwareById() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        OtaPackageInfo savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString()
                + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);

        OtaPackage foundFirmware = doGet("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString(), OtaPackage.class);
        Assert.assertNotNull(foundFirmware);
        Assert.assertEquals(savedFirmware, foundFirmware);
        Assert.assertEquals(DATA, foundFirmware.getData());
    }

    @Test
    public void testDeleteFirmware() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion(VERSION);
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

        Mockito.reset(tbClusterService, auditLogService);

        String idStr = savedFirmwareInfo.getId().getId().toString();
        doDelete("/api/otaPackage/" + idStr)
                .andExpect(status().isOk());

        testNotifyEntityEntityGroupNullAllOneTime(savedFirmwareInfo, savedFirmwareInfo.getId(), savedFirmwareInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, idStr);

        String expected = "Ota package with id [" + idStr + "] is not found";
        doGet("/api/otaPackage/info/" + idStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(expected)));
    }

    @Test
    public void testFindTenantFirmwares() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        List<OtaPackageInfo> otaPackages = new ArrayList<>();
        int cntEntity = 165;
        int startIndexSaveData = 101;
        for (int i = 0; i < cntEntity; i++) {
            SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i >= startIndexSaveData) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
            }
            otaPackages.add(savedFirmwareInfo);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new OtaPackageInfo(), new OtaPackageInfo(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, 0, (cntEntity * 2 - startIndexSaveData));

        List<OtaPackageInfo> loadedFirmwares = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedFirmwares.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackages, idComparator);
        Collections.sort(loadedFirmwares, idComparator);

        Assert.assertEquals(otaPackages, loadedFirmwares);
    }

    @Test
    public void testFindAllFirmwares_CustomerUserWithoutPermission() throws Exception {
        OtaPackageInfo savedFirmwareInfo = createOtaPackageInfo();
        String apiOtaPackagesByProfileId = "/api/otaPackages/" + savedFirmwareInfo.getDeviceProfileId().toString() + "/FIRMWARE?";

        PageLink pageLink = new PageLink(24);
        String apiOtaPackages = "/api/otaPackages?";
        PageData<OtaPackageInfo> pageData = doGetTypedWithPageLink(apiOtaPackages,
                new TypeReference<>() {
                }, pageLink);
        Assert.assertEquals(savedFirmwareInfo, pageData.getData().get(0));

        pageData = doGetTypedWithPageLink(apiOtaPackagesByProfileId,
                new TypeReference<>() {
                }, pageLink);
        Assert.assertEquals(savedFirmwareInfo, pageData.getData().get(0));

        loginNewCustomerUserWithoutPermissions();

        Object[] vars = {pageLink.getPageSize(), pageLink.getPage()};
        String urlTemplate = apiOtaPackages + "pageSize={pageSize}&page={page}";
        doGet(urlTemplate, vars)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("You don't have permission to perform 'READ' operation with 'OTA_PACKAGE' resource!")));

        urlTemplate = apiOtaPackagesByProfileId + "pageSize={pageSize}&page={page}";
        doGet(urlTemplate, vars)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("You don't have permission to perform 'READ' operation with 'OTA_PACKAGE' resource!")));
    }

    @Test
    public void testFindFirmwareById_CustomerUserWithoutPermission() throws Exception {
        OtaPackageInfo savedFirmwareInfo = createOtaPackageInfo();
        String urlTemplateOtaPackageInfoById = "/api/otaPackage/info/" + savedFirmwareInfo.getId().toString();

        doGet(urlTemplateOtaPackageInfoById)
                .andExpect(status().isOk());

        loginNewCustomerUserWithoutPermissions();

        doGet(urlTemplateOtaPackageInfoById)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString("You don't have permission to perform 'READ' operation with OTA_PACKAGE 'My firmware'!")));
    }

    @Test
    public void testFindAllFirmwares_CustomerUserWithPermission() throws Exception {
        OtaPackageInfo savedFirmwareInfo = createOtaPackageInfo();
        String apiOtaPackagesByProfileId = "/api/otaPackages/" + savedFirmwareInfo.getDeviceProfileId().toString() + "/FIRMWARE?";

        PageLink pageLink = new PageLink(24);
        String apiOtaPackages = "/api/otaPackages?";
        PageData<OtaPackageInfo> pageData = doGetTypedWithPageLink(apiOtaPackages,
                new TypeReference<>() {
                }, pageLink);
        Assert.assertEquals(savedFirmwareInfo, pageData.getData().get(0));

        pageData = doGetTypedWithPageLink(apiOtaPackagesByProfileId,
                new TypeReference<>() {
                }, pageLink);
        Assert.assertEquals(savedFirmwareInfo, pageData.getData().get(0));

        loginNewCustomerUserWithPermissions();

        Object[] vars = {pageLink.getPageSize(), pageLink.getPage()};
        String urlTemplate = apiOtaPackages + "pageSize={pageSize}&page={page}";
        doGet(urlTemplate, vars)
                .andExpect(status().isOk());

        urlTemplate = apiOtaPackagesByProfileId + "pageSize={pageSize}&page={page}";
        doGet(urlTemplate, vars)
                .andExpect(status().isOk());
    }

    @Test
    public void testFindFirmwareById_CustomerUserWithPermission() throws Exception {
        OtaPackageInfo savedFirmwareInfo = createOtaPackageInfo();
        String urlTemplateOtaPackageInfoById = "/api/otaPackage/info/" + savedFirmwareInfo.getId().toString();

        OtaPackageInfo actualFirmwareInfo = doGet(urlTemplateOtaPackageInfoById, OtaPackageInfo.class);
        Assert.assertEquals(savedFirmwareInfo, actualFirmwareInfo);

        loginNewCustomerUserWithPermissions();

        actualFirmwareInfo = doGet(urlTemplateOtaPackageInfoById, OtaPackageInfo.class);
        Assert.assertEquals(savedFirmwareInfo, actualFirmwareInfo);
    }

    @Test
    public void testFindTenantFirmwaresByHasData() throws Exception {
        List<OtaPackageInfo> otaPackagesWithData = new ArrayList<>();
        List<OtaPackageInfo> allOtaPackages = new ArrayList<>();

        for (int i = 0; i < 165; i++) {
            SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
            firmwareInfo.setDeviceProfileId(deviceProfileId);
            firmwareInfo.setType(FIRMWARE);
            firmwareInfo.setTitle(TITLE);
            firmwareInfo.setVersion(VERSION + i);
            firmwareInfo.setUsesUrl(false);

            OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);

            if (i > 100) {
                MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

                OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
                savedFirmwareInfo = new OtaPackageInfo(savedFirmware);
                otaPackagesWithData.add(savedFirmwareInfo);
            }

            allOtaPackages.add(savedFirmwareInfo);
        }

        List<OtaPackageInfo> loadedOtaPackagesWithData = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<OtaPackageInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages/" + deviceProfileId.toString() + "/FIRMWARE?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedOtaPackagesWithData.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        List<OtaPackageInfo> allLoadedOtaPackages = new ArrayList<>();
        pageLink = new PageLink(24);
        do {
            pageData = doGetTypedWithPageLink("/api/otaPackages?",
                    new TypeReference<>() {
                    }, pageLink);
            allLoadedOtaPackages.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(otaPackagesWithData, idComparator);
        Collections.sort(allOtaPackages, idComparator);
        Collections.sort(loadedOtaPackagesWithData, idComparator);
        Collections.sort(allLoadedOtaPackages, idComparator);

        Assert.assertEquals(otaPackagesWithData, loadedOtaPackagesWithData);
        Assert.assertEquals(allOtaPackages, allLoadedOtaPackages);
    }

    private OtaPackageInfo save(SaveOtaPackageInfoRequest firmwareInfo) throws Exception {
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    protected OtaPackage savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackage.class);
    }

    private OtaPackageInfo createOtaPackageInfo() throws Exception {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle(TITLE);
        firmwareInfo.setVersion("Test without role");
        firmwareInfo.setUsesUrl(false);

        OtaPackageInfo savedFirmwareInfo = save(firmwareInfo);
        MockMultipartFile testData = new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, DATA.array());

        OtaPackage savedFirmware = savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, CHECKSUM_ALGORITHM);
        return new OtaPackageInfo(savedFirmware);

    }

    private void loginNewCustomerUserWithoutPermissions() throws Exception {
        Customer savedCustomer = createCustomer();
        EntityGroup customerUserGroup = createCustomerUserGroup(savedCustomer);
        EntityGroupInfo savedUserGroupInfo =
                doPostWithResponse("/api/entityGroup", customerUserGroup, EntityGroupInfo.class);

        String pwd = "userWithoutRole";
        User savedCustomerUser = createCustomerUser(savedCustomer, savedUserGroupInfo, "customerUserWithoutRole@thingsboard.org", pwd);
        loginUser(savedCustomerUser.getName(), pwd);
    }

    private void loginNewCustomerUserWithPermissions() throws Exception {
        Customer savedCustomer = createCustomer();
        EntityGroup customerUserGroup = createCustomerUserGroup(savedCustomer);

        Map<Resource, List<Operation>> permissions = Map.of(Resource.OTA_PACKAGE, List.of(Operation.READ));
        Role genericRole = new Role();
        genericRole.setTenantId(savedTenant.getId());
        genericRole.setName("Read Generic Role");
        genericRole.setType(RoleType.GENERIC);
        genericRole.setPermissions(JacksonUtil.valueToTree(permissions));
        genericRole = doPost("/api/role", genericRole, Role.class);

        GroupPermission genericPermission = new GroupPermission();
        genericPermission.setRoleId(genericRole.getId());
        genericPermission.setUserGroupId(customerUserGroup.getId());
        doPost("/api/groupPermission", genericPermission, GroupPermission.class);

        EntityGroupInfo savedUserGroupInfo =
                doPostWithResponse("/api/entityGroup", customerUserGroup, EntityGroupInfo.class);

        String pwd = "userWithAllRole";
        User savedCustomerUser = createCustomerUser(savedCustomer, savedUserGroupInfo, "customerUserWithAllRole@thingsboard.org", pwd);
        loginUser(savedCustomerUser.getName(), pwd);
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setOwnerId(savedTenant.getId());
        customer.setTenantId(savedTenant.getId());
        customer.setParentCustomerId(null);
        customer.setTitle("Customer");
        return doPost("/api/customer", customer, Customer.class);
    }

    private EntityGroup createCustomerUserGroup(Customer savedCustomer) {
        EntityGroup customerUserGroup = new EntityGroup();
        customerUserGroup.setType(EntityType.USER);
        customerUserGroup.setName("Customer User Group");
        customerUserGroup.setOwnerId(savedCustomer.getOwnerId());
        return doPost("/api/entityGroup", customerUserGroup, EntityGroup.class);
    }

    private User createCustomerUser(Customer savedCustomer, EntityGroupInfo savedUserGroupInfo, String email, String pwd) throws Exception {
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(savedCustomer.getId());
        user.setEmail(email);
        return createUser(user, pwd, savedUserGroupInfo.getId());
    }

}

