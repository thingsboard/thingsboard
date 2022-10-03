/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.service.mail.TestMailService;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRequest;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@Slf4j
public abstract class AbstractWebTest extends AbstractInMemoryStorageTest {
    public static final int TIMEOUT = 30;

    protected ObjectMapper mapper = new ObjectMapper();

    protected static final String TEST_TENANT_NAME = "TEST TENANT";
    protected static final String TEST_DIFFERENT_TENANT_NAME = "TEST DIFFERENT TENANT";

    protected static final String SYS_ADMIN_EMAIL = "sysadmin@thingsboard.org";
    private static final String SYS_ADMIN_PASSWORD = "sysadmin";

    protected static final String TENANT_ADMIN_EMAIL = "testtenant@thingsboard.org";
    protected static final String TENANT_ADMIN_PASSWORD = "tenant";

    protected static final String DIFFERENT_TENANT_ADMIN_EMAIL = "testdifftenant@thingsboard.org";
    private static final String DIFFERENT_TENANT_ADMIN_PASSWORD = "difftenant";

    protected static final String CUSTOMER_USER_EMAIL = "testcustomer@thingsboard.org";
    private static final String CUSTOMER_USER_PASSWORD = "customer";

    protected static final String DIFFERENT_CUSTOMER_USER_EMAIL = "testdifferentcustomer@thingsboard.org";
    private static final String DIFFERENT_CUSTOMER_USER_PASSWORD = "diffcustomer";

    /**
     * See {@link org.springframework.test.web.servlet.DefaultMvcResult#getAsyncResult(long)}
     * and {@link org.springframework.mock.web.MockAsyncContext#getTimeout()}
     */
    private static final long DEFAULT_TIMEOUT = -1L;

    protected MediaType contentType = MediaType.APPLICATION_JSON;

    protected MockMvc mockMvc;

    protected String token;
    protected String refreshToken;
    protected String username;

    protected TenantId tenantId;
    protected UserId tenantAdminUserId;
    protected CustomerId tenantAdminCustomerId;
    protected CustomerId customerId;
    protected TenantId differentTenantId;
    protected CustomerId differentCustomerId;
    protected UserId customerUserId;

    @SuppressWarnings("rawtypes")
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @SuppressWarnings("rawtypes")
    private HttpMessageConverter stringHttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            log.info("Starting test: {}", description.getMethodName());
        }

        protected void finished(Description description) {
            log.info("Finished test: {}", description.getMethodName());
        }
    };

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .get();

        this.stringHttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof StringHttpMessageConverter)
                .findAny()
                .get();

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setupWebTest() throws Exception {
        log.info("Executing web test setup");

        if (this.mockMvc == null) {
            this.mockMvc = webAppContextSetup(webApplicationContext)
                    .apply(springSecurity()).build();
        }
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle(TEST_TENANT_NAME);
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail(TENANT_ADMIN_EMAIL);

        tenantAdmin = createUserAndLogin(tenantAdmin, TENANT_ADMIN_PASSWORD);
        tenantAdminUserId = tenantAdmin.getId();
        tenantAdminCustomerId = tenantAdmin.getCustomerId();

        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        customerId = savedCustomer.getId();

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail(CUSTOMER_USER_EMAIL);

        customerUser = createUserAndLogin(customerUser, CUSTOMER_USER_PASSWORD);
        customerUserId = customerUser.getId();

        logout();

        log.info("Executed web test setup");
    }

    @After
    public void teardownWebTest() throws Exception {
        log.info("Executing web test teardown");

        loginSysAdmin();
        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());
        deleteDifferentTenant();

        verifyNoTenantsLeft();

        tenantProfileService.deleteTenantProfiles(TenantId.SYS_TENANT_ID);

        log.info("Executed web test teardown");
    }

    void verifyNoTenantsLeft() throws Exception {
        List<Tenant> loadedTenants = new ArrayList<>();
        PageLink pageLink = new PageLink(10);
        PageData<Tenant> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<PageData<Tenant>>() {
            }, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(loadedTenants).as("All tenants expected to be deleted, but some tenants left in the database").isEmpty();
    }

    protected void loginSysAdmin() throws Exception {
        login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
    }

    protected void loginTenantAdmin() throws Exception {
        login(TENANT_ADMIN_EMAIL, TENANT_ADMIN_PASSWORD);
    }

    protected void loginCustomerUser() throws Exception {
        login(CUSTOMER_USER_EMAIL, CUSTOMER_USER_PASSWORD);
    }

    protected void loginUser(String userName, String password) throws Exception {
        login(userName, password);
    }

    protected Tenant savedDifferentTenant;
    protected User savedDifferentTenantUser;
    private Customer savedDifferentCustomer;

    protected void loginDifferentTenant() throws Exception {
        if (savedDifferentTenant != null) {
            login(DIFFERENT_TENANT_ADMIN_EMAIL, DIFFERENT_TENANT_ADMIN_PASSWORD);
        } else {
            loginSysAdmin();

            Tenant tenant = new Tenant();
            tenant.setTitle(TEST_DIFFERENT_TENANT_NAME);
            savedDifferentTenant = doPost("/api/tenant", tenant, Tenant.class);
            differentTenantId = savedDifferentTenant.getId();
            Assert.assertNotNull(savedDifferentTenant);
            User differentTenantAdmin = new User();
            differentTenantAdmin.setAuthority(Authority.TENANT_ADMIN);
            differentTenantAdmin.setTenantId(savedDifferentTenant.getId());
            differentTenantAdmin.setEmail(DIFFERENT_TENANT_ADMIN_EMAIL);
            savedDifferentTenantUser = createUserAndLogin(differentTenantAdmin, DIFFERENT_TENANT_ADMIN_PASSWORD);
        }
    }

    protected void loginDifferentCustomer() throws Exception {
        if (savedDifferentCustomer != null) {
            login(savedDifferentCustomer.getEmail(), CUSTOMER_USER_PASSWORD);
        } else {
            createDifferentCustomer();

            loginTenantAdmin();
            User differentCustomerUser = new User();
            differentCustomerUser.setAuthority(Authority.CUSTOMER_USER);
            differentCustomerUser.setTenantId(tenantId);
            differentCustomerUser.setCustomerId(savedDifferentCustomer.getId());
            differentCustomerUser.setEmail(DIFFERENT_CUSTOMER_USER_EMAIL);

            createUserAndLogin(differentCustomerUser, DIFFERENT_CUSTOMER_USER_PASSWORD);
        }
    }

    protected void createDifferentCustomer() throws Exception {
        loginTenantAdmin();

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        savedDifferentCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertNotNull(savedDifferentCustomer);
        differentCustomerId = savedDifferentCustomer.getId();

        logout();
    }

    protected void deleteDifferentTenant() throws Exception {
        if (savedDifferentTenant != null) {
            loginSysAdmin();
            doDelete("/api/tenant/" + savedDifferentTenant.getId().getId().toString())
                    .andExpect(status().isOk());
            savedDifferentTenant = null;
        }
    }

    protected User createUserAndLogin(User user, String password) throws Exception {
        User savedUser = doPost("/api/user", user, User.class);
        logout();
        JsonNode activateRequest = getActivateRequest(password);
        JsonNode tokenInfo = readResponse(doPost("/api/noauth/activate", activateRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, user.getEmail());
        return savedUser;
    }

    protected User createUser(User user, String password) throws Exception {
        User savedUser = doPost("/api/user", user, User.class);
        JsonNode activateRequest = getActivateRequest(password);
        ResultActions resultActions = doPost("/api/noauth/activate", activateRequest);
        resultActions.andExpect(status().isOk());
        return savedUser;
    }

    private JsonNode getActivateRequest(String password) throws Exception {
        doGet("/api/noauth/activate?activateToken={activateToken}", TestMailService.currentActivateToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/createPassword?activateToken=" + TestMailService.currentActivateToken));
        return new ObjectMapper().createObjectNode()
                .put("activateToken", TestMailService.currentActivateToken)
                .put("password", password);
    }

    protected void login(String username, String password) throws Exception {
        this.token = null;
        this.refreshToken = null;
        this.username = null;
        JsonNode tokenInfo = readResponse(doPost("/api/auth/login", new LoginRequest(username, password)).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, username);
    }

    protected void refreshToken() throws Exception {
        this.token = null;
        JsonNode tokenInfo = readResponse(doPost("/api/auth/token", new RefreshTokenRequest(this.refreshToken)).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, this.username);
    }

    protected void validateAndSetJwtToken(JsonNode tokenInfo, String username) {
        Assert.assertNotNull(tokenInfo);
        Assert.assertTrue(tokenInfo.has("token"));
        Assert.assertTrue(tokenInfo.has("refreshToken"));
        String token = tokenInfo.get("token").asText();
        String refreshToken = tokenInfo.get("refreshToken").asText();
        validateJwtToken(token, username);
        validateJwtToken(refreshToken, username);
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    protected void validateJwtToken(String token, String username) {
        Assert.assertNotNull(token);
        Assert.assertFalse(token.isEmpty());
        int i = token.lastIndexOf('.');
        Assert.assertTrue(i > 0);
        String withoutSignature = token.substring(0, i + 1);
        Jwt<Header, Claims> jwsClaims = Jwts.parser().parseClaimsJwt(withoutSignature);
        Claims claims = jwsClaims.getBody();
        String subject = claims.getSubject();
        Assert.assertEquals(username, subject);
    }

    protected void logout() throws Exception {
        this.token = null;
        this.refreshToken = null;
        this.username = null;
    }

    protected void setJwtToken(MockHttpServletRequestBuilder request) {
        if (this.token != null) {
            request.header(ThingsboardSecurityConfiguration.JWT_TOKEN_HEADER_PARAM, "Bearer " + this.token);
        }
    }

    protected DeviceProfile createDeviceProfile(String name) {
        return createDeviceProfile(name, null);
    }

    protected DeviceProfile createDeviceProfile(String name, DeviceProfileTransportConfiguration deviceProfileTransportConfiguration) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setDescription(name + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        deviceProfileData.setConfiguration(configuration);
        if (deviceProfileTransportConfiguration != null) {
            deviceProfile.setTransportType(deviceProfileTransportConfiguration.getType());
            deviceProfileData.setTransportConfiguration(deviceProfileTransportConfiguration);
        } else {
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
            deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        }
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    protected AssetProfile createAssetProfile(String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(name);
        assetProfile.setDescription(name + " Test");
        assetProfile.setDefault(false);
        assetProfile.setDefaultRuleChainId(null);
        return assetProfile;
    }

    protected MqttDeviceProfileTransportConfiguration createMqttDeviceProfileTransportConfiguration(TransportPayloadTypeConfiguration transportPayloadTypeConfiguration, boolean sendAckOnValidationException) {
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
        mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(MqttTopics.DEVICE_TELEMETRY_TOPIC);
        mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
        mqttDeviceProfileTransportConfiguration.setSendAckOnValidationException(sendAckOnValidationException);
        mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        return mqttDeviceProfileTransportConfiguration;
    }

    protected ProtoTransportPayloadConfiguration createProtoTransportPayloadConfiguration(String attributesProtoSchema, String telemetryProtoSchema, String rpcRequestProtoSchema, String rpcResponseProtoSchema) {
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
        protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(attributesProtoSchema);
        protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(telemetryProtoSchema);
        protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(rpcRequestProtoSchema);
        protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(rpcResponseProtoSchema);
        return protoTransportPayloadConfiguration;
    }


    protected ResultActions doGet(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(getRequest);
    }

    protected <T> T doGet(String urlTemplate, Class<T> responseClass, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(status().isOk()), responseClass);
    }

    protected <T> T doGet(String urlTemplate, Class<T> responseClass, ResultMatcher resultMatcher, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doGetAsync(String urlTemplate, Class<T> responseClass, Object... urlVariables) throws Exception {
        return readResponse(doGetAsync(urlTemplate, urlVariables).andExpect(status().isOk()), responseClass);
    }

    protected <T> T doGetAsyncTyped(String urlTemplate, TypeReference<T> responseType, Object... urlVariables) throws Exception {
        return readResponse(doGetAsync(urlTemplate, urlVariables).andExpect(status().isOk()), responseType);
    }

    protected ResultActions doGetAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest;
        getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

    protected <T> T doGetTyped(String urlTemplate, TypeReference<T> responseType, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(status().isOk()), responseType);
    }

    protected <T> T doGetTypedWithPageLink(String urlTemplate, TypeReference<T> responseType,
                                           PageLink pageLink,
                                           Object... urlVariables) throws Exception {
        List<Object> pageLinkVariables = new ArrayList<>();
        urlTemplate += "pageSize={pageSize}&page={page}";
        pageLinkVariables.add(pageLink.getPageSize());
        pageLinkVariables.add(pageLink.getPage());
        if (StringUtils.isNotEmpty(pageLink.getTextSearch())) {
            urlTemplate += "&textSearch={textSearch}";
            pageLinkVariables.add(pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            urlTemplate += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
            pageLinkVariables.add(pageLink.getSortOrder().getProperty());
            pageLinkVariables.add(pageLink.getSortOrder().getDirection().name());
        }

        Object[] vars = new Object[urlVariables.length + pageLinkVariables.size()];
        System.arraycopy(urlVariables, 0, vars, 0, urlVariables.length);
        System.arraycopy(pageLinkVariables.toArray(), 0, vars, urlVariables.length, pageLinkVariables.size());

        return readResponse(doGet(urlTemplate, vars).andExpect(status().isOk()), responseType);
    }

    protected <T> T doGetTypedWithTimePageLink(String urlTemplate, TypeReference<T> responseType,
                                               TimePageLink pageLink,
                                               Object... urlVariables) throws Exception {
        List<Object> pageLinkVariables = new ArrayList<>();
        urlTemplate += "pageSize={pageSize}&page={page}";
        pageLinkVariables.add(pageLink.getPageSize());
        pageLinkVariables.add(pageLink.getPage());
        if (pageLink.getStartTime() != null) {
            urlTemplate += "&startTime={startTime}";
            pageLinkVariables.add(pageLink.getStartTime());
        }
        if (pageLink.getEndTime() != null) {
            urlTemplate += "&endTime={endTime}";
            pageLinkVariables.add(pageLink.getEndTime());
        }
        if (StringUtils.isNotEmpty(pageLink.getTextSearch())) {
            urlTemplate += "&textSearch={textSearch}";
            pageLinkVariables.add(pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            urlTemplate += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
            pageLinkVariables.add(pageLink.getSortOrder().getProperty());
            pageLinkVariables.add(pageLink.getSortOrder().getDirection().name());
        }
        Object[] vars = new Object[urlVariables.length + pageLinkVariables.size()];
        System.arraycopy(urlVariables, 0, vars, 0, urlVariables.length);
        System.arraycopy(pageLinkVariables.toArray(), 0, vars, urlVariables.length, pageLinkVariables.size());

        return readResponse(doGet(urlTemplate, vars).andExpect(status().isOk()), responseType);
    }

    protected <T> T doPost(String urlTemplate, Class<T> responseClass, String... params) {
        try {
            return readResponse(doPost(urlTemplate, params).andExpect(status().isOk()), responseClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T doPost(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doPost(String urlTemplate, T content, Class<T> responseClass, String... params) {
        try {
            return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T, R> R doPostWithResponse(String urlTemplate, T content, Class<R> responseClass, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseClass);
    }

    protected <T, R> R doPostWithTypedResponse(String urlTemplate, T content, TypeReference<R> responseType, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseType);
    }

    protected <T, R> R doPostWithTypedResponse(String urlTemplate, T content, TypeReference<R> responseType, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(resultMatcher), responseType);
    }

    protected <T> T doPostAsync(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, DEFAULT_TIMEOUT, params).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doPostAsync(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, Long timeout, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, timeout, params).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doPostClaimAsync(String urlTemplate, Object content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, DEFAULT_TIMEOUT, params).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doDelete(String urlTemplate, Class<T> responseClass, String... params) throws Exception {
        return readResponse(doDelete(urlTemplate, params).andExpect(status().isOk()), responseClass);
    }

    protected ResultActions doPost(String urlTemplate, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate);
        setJwtToken(postRequest);
        populateParams(postRequest, params);
        return mockMvc.perform(postRequest);
    }

    protected <T> ResultActions doPost(String urlTemplate, T content, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate, params);
        setJwtToken(postRequest);
        String json = json(content);
        postRequest.contentType(contentType).content(json);
        return mockMvc.perform(postRequest);
    }

    protected <T> ResultActions doPostAsync(String urlTemplate, T content, Long timeout, String... params) throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate, params);
        setJwtToken(postRequest);
        String json = json(content);
        postRequest.contentType(contentType).content(json);
        MvcResult result = mockMvc.perform(postRequest).andReturn();
        result.getAsyncResult(timeout);
        return mockMvc.perform(asyncDispatch(result));
    }

    protected ResultActions doDelete(String urlTemplate, String... params) throws Exception {
        MockHttpServletRequestBuilder deleteRequest = delete(urlTemplate);
        setJwtToken(deleteRequest);
        populateParams(deleteRequest, params);
        return mockMvc.perform(deleteRequest);
    }

    protected void populateParams(MockHttpServletRequestBuilder request, String... params) {
        if (params != null && params.length > 0) {
            Assert.assertEquals(0, params.length % 2);
            MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
            for (int i = 0; i < params.length; i += 2) {
                paramsMap.add(params[i], params[i + 1]);
            }
            request.params(paramsMap);
        }
    }

    @SuppressWarnings("unchecked")
    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();

        HttpMessageConverter converter = o instanceof String ? stringHttpMessageConverter : mappingJackson2HttpMessageConverter;
        converter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    @SuppressWarnings("unchecked")
    protected <T> T readResponse(ResultActions result, Class<T> responseClass) throws Exception {
        byte[] content = result.andReturn().getResponse().getContentAsByteArray();
        MockHttpInputMessage mockHttpInputMessage = new MockHttpInputMessage(content);
        HttpMessageConverter converter = responseClass.equals(String.class) ? stringHttpMessageConverter : mappingJackson2HttpMessageConverter;
        return (T) converter.read(responseClass, mockHttpInputMessage);
    }

    protected <T> T readResponse(ResultActions result, TypeReference<T> type) throws Exception {
        return readResponse(result.andReturn(), type);
    }

    protected <T> T readResponse(MvcResult result, TypeReference<T> type) throws Exception {
        byte[] content = result.getResponse().getContentAsByteArray();
        return mapper.readerFor(type).readValue(content);
    }

    protected String getErrorMessage(ResultActions result) throws Exception {
        return readResponse(result, JsonNode.class).get("message").asText();
    }

    public class IdComparator<D extends HasId> implements Comparator<D> {
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }

    protected static <T> ResultMatcher statusReason(Matcher<T> matcher) {
        return jsonPath("$.message", matcher);
    }

    protected Edge constructEdge(String name, String type) {
        return constructEdge(tenantId, name, type);
    }

    protected Edge constructEdge(TenantId tenantId, String name, String type) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(name);
        edge.setType(type);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        return edge;
    }

    protected <T extends HasId<? extends UUIDBased>> ListenableFuture<List<ResultActions>> deleteEntitiesAsync(String urlTemplate, List<T> entities, ListeningExecutorService executor) {
        List<ListenableFuture<ResultActions>> futures = new ArrayList<>(entities.size());
        for (T entity : entities) {
            futures.add(executor.submit(() ->
                    doDelete(urlTemplate + entity.getId().getId())
                            .andExpect(status().isOk())));
        }
        return Futures.allAsList(futures);
    }

    protected void testEntityDaoWithRelationsOk(EntityId entityIdFrom, EntityId entityTo, String urlDelete) throws Exception {
        createEntityRelation(entityIdFrom, entityTo, "TEST_TYPE");
        assertThat(findRelationsByTo(entityTo)).hasSize(1);

        doDelete(urlDelete).andExpect(status().isOk());

        assertThat(findRelationsByTo(entityTo)).hasSize(0);
    }

    protected <T> void testEntityDaoWithRelationsTransactionalException(Dao<T> dao, EntityId entityIdFrom, EntityId entityTo,
                                                                        String urlDelete) throws Exception {
        Mockito.doThrow(new ConstraintViolationException("mock message", new SQLException(), "MOCK_CONSTRAINT")).when(dao).removeById(any(), any());
        try {
            createEntityRelation(entityIdFrom, entityTo, "TEST_TRANSACTIONAL_TYPE");
            assertThat(findRelationsByTo(entityTo)).hasSize(1);

            doDelete(urlDelete)
                    .andExpect(status().isInternalServerError());

            assertThat(findRelationsByTo(entityTo)).hasSize(1);
        } finally {
            Mockito.reset(dao);
        }
    }

    protected void createEntityRelation(EntityId entityIdFrom, EntityId entityIdTo, String typeRelation) throws Exception {
        EntityRelation relation = new EntityRelation(entityIdFrom, entityIdTo, typeRelation);
        doPost("/api/relation", relation);
    }

    protected List<EntityRelation> findRelationsByTo(EntityId entityId) throws Exception {
        String url = String.format("/api/relations?toId=%s&toType=%s", entityId.getId(), entityId.getEntityType().name());
        MvcResult mvcResult = doGet(url).andReturn();

        switch (mvcResult.getResponse().getStatus()) {
            case 200:
                return readResponse(mvcResult, new TypeReference<>() {
                });
            case 404:
                return Collections.emptyList();
        }
        throw new AssertionError("Unexpected status " + mvcResult.getResponse().getStatus());
    }

}
