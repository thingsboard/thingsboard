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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.ThingsboardSecurityConfiguration;
import org.thingsboard.server.service.mail.TestMailService;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRequest;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractControllerTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"org.thingsboard.server"})
@WebAppConfiguration
@SpringBootTest
@Slf4j
public abstract class AbstractControllerTest {

    protected ObjectMapper mapper = new ObjectMapper();

    protected static final String TEST_TENANT_NAME = "TEST TENANT";

    protected static final String SYS_ADMIN_EMAIL = "sysadmin@thingsboard.org";
    private static final String SYS_ADMIN_PASSWORD = "sysadmin";

    protected static final String TENANT_ADMIN_EMAIL = "testtenant@thingsboard.org";
    private static final String TENANT_ADMIN_PASSWORD = "tenant";

    protected static final String CUSTOMER_USER_EMAIL = "testcustomer@thingsboard.org";
    private static final String CUSTOMER_USER_PASSWORD = "customer";

    /** See {@link org.springframework.test.web.servlet.DefaultMvcResult#getAsyncResult(long)}
     *  and {@link org.springframework.mock.web.MockAsyncContext#getTimeout()}
     */
    private static final long DEFAULT_TIMEOUT = -1L;

    protected MediaType contentType = MediaType.APPLICATION_JSON;

    protected MockMvc mockMvc;

    protected String token;
    protected String refreshToken;
    protected String username;

    private TenantId tenantId;

    @SuppressWarnings("rawtypes")
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @SuppressWarnings("rawtypes")
    private HttpMessageConverter stringHttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

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
    public void setup() throws Exception {
        log.info("Executing setup");
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

        createUserAndLogin(tenantAdmin, TENANT_ADMIN_PASSWORD);

        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail(CUSTOMER_USER_EMAIL);

        createUserAndLogin(customerUser, CUSTOMER_USER_PASSWORD);

        logout();

        log.info("Executed setup");
    }

    @After
    public void teardown() throws Exception {
        log.info("Executing teardown");
        loginSysAdmin();
        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());
        log.info("Executed teardown");
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

    protected User createUserAndLogin(User user, String password) throws Exception {
        User savedUser = doPost("/api/user", user, User.class);
        logout();
        doGet("/api/noauth/activate?activateToken={activateToken}", TestMailService.currentActivateToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/createPassword?activateToken=" + TestMailService.currentActivateToken));
        JsonNode activateRequest = new ObjectMapper().createObjectNode()
                .put("activateToken", TestMailService.currentActivateToken)
                .put("password", password);
        JsonNode tokenInfo = readResponse(doPost("/api/noauth/activate", activateRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, user.getEmail());
        return savedUser;
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

    protected ResultActions doGet(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(getRequest);
    }

    protected <T> T doGet(String urlTemplate, Class<T> responseClass, Object... urlVariables) throws Exception {
        return readResponse(doGet(urlTemplate, urlVariables).andExpect(status().isOk()), responseClass);
    }

    protected <T> T doGetAsync(String urlTemplate, Class<T> responseClass, Object... urlVariables) throws Exception {
        return readResponse(doGetAsync(urlTemplate, urlVariables).andExpect(status().isOk()), responseClass);
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
                                           TextPageLink pageLink,
                                           Object... urlVariables) throws Exception {
        List<Object> pageLinkVariables = new ArrayList<>();
        urlTemplate += "limit={limit}";
        pageLinkVariables.add(pageLink.getLimit());
        if (StringUtils.isNotEmpty(pageLink.getTextSearch())) {
            urlTemplate += "&textSearch={textSearch}";
            pageLinkVariables.add(pageLink.getTextSearch());
        }
        if (pageLink.getIdOffset() != null) {
            urlTemplate += "&idOffset={idOffset}";
            pageLinkVariables.add(pageLink.getIdOffset().toString());
        }
        if (StringUtils.isNotEmpty(pageLink.getTextOffset())) {
            urlTemplate += "&textOffset={textOffset}";
            pageLinkVariables.add(pageLink.getTextOffset());
        }

        Object[] vars = new Object[urlVariables.length + pageLinkVariables.size()];
        System.arraycopy(urlVariables, 0, vars, 0, urlVariables.length);
        System.arraycopy(pageLinkVariables.toArray(), 0, vars, urlVariables.length, pageLinkVariables.size());

        return readResponse(doGet(urlTemplate, vars).andExpect(status().isOk()), responseType);
    }

    protected <T> T  doGetTypedWithTimePageLink(String urlTemplate, TypeReference<T> responseType,
                                                TimePageLink pageLink,
                                                Object... urlVariables) throws Exception {
        List<Object> pageLinkVariables = new ArrayList<>();
        urlTemplate += "limit={limit}";
        pageLinkVariables.add(pageLink.getLimit());
        if (pageLink.getStartTime() != null) {
            urlTemplate += "&startTime={startTime}";
            pageLinkVariables.add(pageLink.getStartTime());
        }
        if (pageLink.getEndTime() != null) {
            urlTemplate += "&endTime={endTime}";
            pageLinkVariables.add(pageLink.getEndTime());
        }
        if (pageLink.getIdOffset() != null) {
            urlTemplate += "&offset={offset}";
            pageLinkVariables.add(pageLink.getIdOffset().toString());
        }
        if (pageLink.isAscOrder()) {
            urlTemplate += "&ascOrder={ascOrder}";
            pageLinkVariables.add(pageLink.isAscOrder());
        }
        Object[] vars = new Object[urlVariables.length + pageLinkVariables.size()];
        System.arraycopy(urlVariables, 0, vars, 0, urlVariables.length);
        System.arraycopy(pageLinkVariables.toArray(), 0, vars, urlVariables.length, pageLinkVariables.size());

        return readResponse(doGet(urlTemplate, vars).andExpect(status().isOk()), responseType);
    }

    protected <T> T doPost(String urlTemplate, Class<T> responseClass, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, params).andExpect(status().isOk()), responseClass);
    }

    protected <T> T doPost(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doPost(String urlTemplate, T content, Class<T> responseClass, String... params) throws Exception {
        return readResponse(doPost(urlTemplate, content, params).andExpect(status().isOk()), responseClass);
    }

    protected <T> T doPostAsync(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, DEFAULT_TIMEOUT, params).andExpect(resultMatcher), responseClass);
    }

    protected <T> T doPostAsync(String urlTemplate, T content, Class<T> responseClass, ResultMatcher resultMatcher, Long timeout, String... params) throws Exception {
        return readResponse(doPostAsync(urlTemplate, content, timeout, params).andExpect(resultMatcher), responseClass);
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
        MockHttpServletRequestBuilder postRequest = post(urlTemplate);
        setJwtToken(postRequest);
        String json = json(content);
        postRequest.contentType(contentType).content(json);
        return mockMvc.perform(postRequest);
    }

    protected <T> ResultActions doPostAsync(String urlTemplate, T content, Long timeout, String... params)  throws Exception {
        MockHttpServletRequestBuilder postRequest = post(urlTemplate);
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
        byte[] content = result.andReturn().getResponse().getContentAsByteArray();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readerFor(type).readValue(content);
    }

    public class IdComparator<D extends BaseData<? extends UUIDBased>> implements Comparator<D> {
        @Override
        public int compare(D o1, D o2) {
            return o1.getId().getId().compareTo(o2.getId().getId());
        }
    }

    protected static <T> ResultMatcher statusReason(Matcher<T> matcher) {
        return jsonPath("$.message", matcher);
    }

}
