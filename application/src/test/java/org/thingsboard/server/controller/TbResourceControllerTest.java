/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TbResourceControllerTest extends AbstractControllerTest {

    private final IdComparator<TbResourceInfo> idComparator = new IdComparator<>();

    private static final String DEFAULT_FILE_NAME = "test.jks";
    private static final String DEFAULT_FILE_NAME_2 = "test2.jks";
    public static final String JS_TEST_FILE_NAME = "test.js";
    public static final String TEST_DATA = "77u/PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPCEtLQpGSUxFIElORk9STUFUSU9OCgpPTUEgUGVybWFuZW50IERvY3VtZW50CiAgIEZpbGU6IE9NQS1TVVAtTHdNMk1fQmluYXJ5QXBwRGF0YUNvbnRhaW5lci1WMV8wXzEtMjAxOTAyMjEtQQogICBUeXBlOiB4bWwKClB1YmxpYyBSZWFjaGFibGUgSW5mb3JtYXRpb24KICAgUGF0aDogaHR0cDovL3d3dy5vcGVubW9iaWxlYWxsaWFuY2Uub3JnL3RlY2gvcHJvZmlsZXMKICAgTmFtZTogTHdNMk1fQmluYXJ5QXBwRGF0YUNvbnRhaW5lci12MV8wXzEueG1sCgpOT1JNQVRJVkUgSU5GT1JNQVRJT04KCiAgSW5mb3JtYXRpb24gYWJvdXQgdGhpcyBmaWxlIGNhbiBiZSBmb3VuZCBpbiB0aGUgbGF0ZXN0IHJldmlzaW9uIG9mCgogIE9NQS1UUy1MV00yTV9CaW5hcnlBcHBEYXRhQ29udGFpbmVyLVYxXzBfMQoKICBUaGlzIGlzIGF2YWlsYWJsZSBhdCBodHRwOi8vd3d3Lm9wZW5tb2JpbGVhbGxpYW5jZS5vcmcvCgogIFNlbmQgY29tbWVudHMgdG8gaHR0cHM6Ly9naXRodWIuY29tL09wZW5Nb2JpbGVBbGxpYW5jZS9PTUFfTHdNMk1fZm9yX0RldmVsb3BlcnMvaXNzdWVzCgpDSEFOR0UgSElTVE9SWQoKMTUwNjIwMTggU3RhdHVzIGNoYW5nZWQgdG8gQXBwcm92ZWQgYnkgRE0sIERvYyBSZWYgIyBPTUEtRE0mU0UtMjAxOC0wMDYxLUlOUF9MV00yTV9BUFBEQVRBX1YxXzBfRVJQX2Zvcl9maW5hbF9BcHByb3ZhbAoyMTAyMjAxOSBTdGF0dXMgY2hhbmdlZCB0byBBcHByb3ZlZCBieSBJUFNPLCBEb2MgUmVmICMgT01BLUlQU08tMjAxOS0wMDI1LUlOUF9Md00yTV9PYmplY3RfQXBwX0RhdGFfQ29udGFpbmVyXzFfMF8xX2Zvcl9GaW5hbF9BcHByb3ZhbAoKTEVHQUwgRElTQ0xBSU1FUgoKQ29weXJpZ2h0IDIwMTkgT3BlbiBNb2JpbGUgQWxsaWFuY2UuCgpSZWRpc3RyaWJ1dGlvbiBhbmQgdXNlIGluIHNvdXJjZSBhbmQgYmluYXJ5IGZvcm1zLCB3aXRoIG9yIHdpdGhvdXQKbW9kaWZpY2F0aW9uLCBhcmUgcGVybWl0dGVkIHByb3ZpZGVkIHRoYXQgdGhlIGZvbGxvd2luZyBjb25kaXRpb25zCmFyZSBtZXQ6CgoxLiBSZWRpc3RyaWJ1dGlvbnMgb2Ygc291cmNlIGNvZGUgbXVzdCByZXRhaW4gdGhlIGFib3ZlIGNvcHlyaWdodApub3RpY2UsIHRoaXMgbGlzdCBvZiBjb25kaXRpb25zIGFuZCB0aGUgZm9sbG93aW5nIGRpc2NsYWltZXIuCjIuIFJlZGlzdHJpYnV0aW9ucyBpbiBiaW5hcnkgZm9ybSBtdXN0IHJlcHJvZHVjZSB0aGUgYWJvdmUgY29weXJpZ2h0Cm5vdGljZSwgdGhpcyBsaXN0IG9mIGNvbmRpdGlvbnMgYW5kIHRoZSBmb2xsb3dpbmcgZGlzY2xhaW1lciBpbiB0aGUKZG9jdW1lbnRhdGlvbiBhbmQvb3Igb3RoZXIgbWF0ZXJpYWxzIHByb3ZpZGVkIHdpdGggdGhlIGRpc3RyaWJ1dGlvbi4KMy4gTmVpdGhlciB0aGUgbmFtZSBvZiB0aGUgY29weXJpZ2h0IGhvbGRlciBub3IgdGhlIG5hbWVzIG9mIGl0cwpjb250cmlidXRvcnMgbWF5IGJlIHVzZWQgdG8gZW5kb3JzZSBvciBwcm9tb3RlIHByb2R1Y3RzIGRlcml2ZWQKZnJvbSB0aGlzIHNvZnR3YXJlIHdpdGhvdXQgc3BlY2lmaWMgcHJpb3Igd3JpdHRlbiBwZXJtaXNzaW9uLgoKVEhJUyBTT0ZUV0FSRSBJUyBQUk9WSURFRCBCWSBUSEUgQ09QWVJJR0hUIEhPTERFUlMgQU5EIENPTlRSSUJVVE9SUwoiQVMgSVMiIEFORCBBTlkgRVhQUkVTUyBPUiBJTVBMSUVEIFdBUlJBTlRJRVMsIElOQ0xVRElORywgQlVUIE5PVApMSU1JVEVEIFRPLCBUSEUgSU1QTElFRCBXQVJSQU5USUVTIE9GIE1FUkNIQU5UQUJJTElUWSBBTkQgRklUTkVTUwpGT1IgQSBQQVJUSUNVTEFSIFBVUlBPU0UgQVJFIERJU0NMQUlNRUQuIElOIE5PIEVWRU5UIFNIQUxMIFRIRQpDT1BZUklHSFQgSE9MREVSIE9SIENPTlRSSUJVVE9SUyBCRSBMSUFCTEUgRk9SIEFOWSBESVJFQ1QsIElORElSRUNULApJTkNJREVOVEFMLCBTUEVDSUFMLCBFWEVNUExBUlksIE9SIENPTlNFUVVFTlRJQUwgREFNQUdFUyAoSU5DTFVESU5HLApCVVQgTk9UIExJTUlURUQgVE8sIFBST0NVUkVNRU5UIE9GIFNVQlNUSVRVVEUgR09PRFMgT1IgU0VSVklDRVM7CkxPU1MgT0YgVVNFLCBEQVRBLCBPUiBQUk9GSVRTOyBPUiBCVVNJTkVTUyBJTlRFUlJVUFRJT04pIEhPV0VWRVIKQ0FVU0VEIEFORCBPTiBBTlkgVEhFT1JZIE9GIExJQUJJTElUWSwgV0hFVEhFUiBJTiBDT05UUkFDVCwgU1RSSUNUCkxJQUJJTElUWSwgT1IgVE9SVCAoSU5DTFVESU5HIE5FR0xJR0VOQ0UgT1IgT1RIRVJXSVNFKSBBUklTSU5HIElOCkFOWSBXQVkgT1VUIE9GIFRIRSBVU0UgT0YgVEhJUyBTT0ZUV0FSRSwgRVZFTiBJRiBBRFZJU0VEIE9GIFRIRQpQT1NTSUJJTElUWSBPRiBTVUNIIERBTUFHRS4KClRoZSBhYm92ZSBsaWNlbnNlIGlzIHVzZWQgYXMgYSBsaWNlbnNlIHVuZGVyIGNvcHlyaWdodCBvbmx5LiBQbGVhc2UKcmVmZXJlbmNlIHRoZSBPTUEgSVBSIFBvbGljeSBmb3IgcGF0ZW50IGxpY2Vuc2luZyB0ZXJtczoKaHR0cHM6Ly93d3cub21hc3BlY3dvcmtzLm9yZy9hYm91dC9pbnRlbGxlY3R1YWwtcHJvcGVydHktcmlnaHRzLwoKLS0+CjxMV00yTSB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIiB4c2k6bm9OYW1lc3BhY2VTY2hlbWFMb2NhdGlvbj0iaHR0cDovL29wZW5tb2JpbGVhbGxpYW5jZS5vcmcvdGVjaC9wcm9maWxlcy9MV00yTS54c2QiPgoJPE9iamVjdCBPYmplY3RUeXBlPSJNT0RlZmluaXRpb24iPgoJCTxOYW1lPkJpbmFyeUFwcERhdGFDb250YWluZXI8L05hbWU+CgkJPERlc2NyaXB0aW9uMT48IVtDREFUQVtUaGlzIEx3TTJNIE9iamVjdHMgcHJvdmlkZXMgdGhlIGFwcGxpY2F0aW9uIHNlcnZpY2UgZGF0YSByZWxhdGVkIHRvIGEgTHdNMk0gU2VydmVyLCBlZy4gV2F0ZXIgbWV0ZXIgZGF0YS4gClRoZXJlIGFyZSBzZXZlcmFsIG1ldGhvZHMgdG8gY3JlYXRlIGluc3RhbmNlIHRvIGluZGljYXRlIHRoZSBtZXNzYWdlIGRpcmVjdGlvbiBiYXNlZCBvbiB0aGUgbmVnb3RpYXRpb24gYmV0d2VlbiBBcHBsaWNhdGlvbiBhbmQgTHdNMk0uIFRoZSBDbGllbnQgYW5kIFNlcnZlciBzaG91bGQgbmVnb3RpYXRlIHRoZSBpbnN0YW5jZShzKSB1c2VkIHRvIGV4Y2hhbmdlIHRoZSBkYXRhLiBGb3IgZXhhbXBsZToKIC0gVXNpbmcgYSBzaW5nbGUgaW5zdGFuY2UgZm9yIGJvdGggZGlyZWN0aW9ucyBjb21tdW5pY2F0aW9uLCBmcm9tIENsaWVudCB0byBTZXJ2ZXIgYW5kIGZyb20gU2VydmVyIHRvIENsaWVudC4KIC0gVXNpbmcgYW4gaW5zdGFuY2UgZm9yIGNvbW11bmljYXRpb24gZnJvbSBDbGllbnQgdG8gU2VydmVyIGFuZCBhbm90aGVyIG9uZSBmb3IgY29tbXVuaWNhdGlvbiBmcm9tIFNlcnZlciB0byBDbGllbnQKIC0gVXNpbmcgc2V2ZXJhbCBpbnN0YW5jZXMKXV0+PC9EZXNjcmlwdGlvbjE+CgkJPE9iamVjdElEPjE5PC9PYmplY3RJRD4KCQk8T2JqZWN0VVJOPnVybjpvbWE6bHdtMm06b21hOjE5PC9PYmplY3RVUk4+CgkJPExXTTJNVmVyc2lvbj4xLjA8L0xXTTJNVmVyc2lvbj4KCQk8T2JqZWN0VmVyc2lvbj4xLjA8L09iamVjdFZlcnNpb24+CgkJPE11bHRpcGxlSW5zdGFuY2VzPk11bHRpcGxlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQk8TWFuZGF0b3J5Pk9wdGlvbmFsPC9NYW5kYXRvcnk+CgkJPFJlc291cmNlcz4KCQkJPEl0ZW0gSUQ9IjAiPjxOYW1lPkRhdGE8L05hbWU+CgkJCQk8T3BlcmF0aW9ucz5SVzwvT3BlcmF0aW9ucz4KCQkJCTxNdWx0aXBsZUluc3RhbmNlcz5NdWx0aXBsZTwvTXVsdGlwbGVJbnN0YW5jZXM+CgkJCQk8TWFuZGF0b3J5Pk1hbmRhdG9yeTwvTWFuZGF0b3J5PgoJCQkJPFR5cGU+T3BhcXVlPC9UeXBlPgoJCQkJPFJhbmdlRW51bWVyYXRpb24gLz4KCQkJCTxVbml0cyAvPgoJCQkJPERlc2NyaXB0aW9uPjwhW0NEQVRBW0luZGljYXRlcyB0aGUgYXBwbGljYXRpb24gZGF0YSBjb250ZW50Ll1dPjwvRGVzY3JpcHRpb24+CgkJCTwvSXRlbT4KCQkJPEl0ZW0gSUQ9IjEiPjxOYW1lPkRhdGEgUHJpb3JpdHk8L05hbWU+CgkJCQk8T3BlcmF0aW9ucz5SVzwvT3BlcmF0aW9ucz4KCQkJCTxNdWx0aXBsZUluc3RhbmNlcz5TaW5nbGU8L011bHRpcGxlSW5zdGFuY2VzPgoJCQkJPE1hbmRhdG9yeT5PcHRpb25hbDwvTWFuZGF0b3J5PgoJCQkJPFR5cGU+SW50ZWdlcjwvVHlwZT4KCQkJCTxSYW5nZUVudW1lcmF0aW9uPjEgYnl0ZXM8L1JhbmdlRW51bWVyYXRpb24+CgkJCQk8VW5pdHMgLz4KCQkJCTxEZXNjcmlwdGlvbj48IVtDREFUQVtJbmRpY2F0ZXMgdGhlIEFwcGxpY2F0aW9uIGRhdGEgcHJpb3JpdHk6CjA6SW1tZWRpYXRlCjE6QmVzdEVmZm9ydAoyOkxhdGVzdAozLTEwMDogUmVzZXJ2ZWQgZm9yIGZ1dHVyZSB1c2UuCjEwMS0yNTQ6IFByb3ByaWV0YXJ5IG1vZGUuXV0+PC9EZXNjcmlwdGlvbj4KCQkJPC9JdGVtPgoJCQk8SXRlbSBJRD0iMiI+PE5hbWU+RGF0YSBDcmVhdGlvbiBUaW1lPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPlRpbWU8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbiAvPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBEYXRhIGluc3RhbmNlIGNyZWF0aW9uIHRpbWVzdGFtcC5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+CgkJCTxJdGVtIElEPSIzIj48TmFtZT5EYXRhIERlc2NyaXB0aW9uPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPlN0cmluZzwvVHlwZT4KCQkJCTxSYW5nZUVudW1lcmF0aW9uPjMyIGJ5dGVzPC9SYW5nZUVudW1lcmF0aW9uPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBkYXRhIGRlc2NyaXB0aW9uLgplLmcuICJtZXRlciByZWFkaW5nIi5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+CgkJCTxJdGVtIElEPSI0Ij48TmFtZT5EYXRhIEZvcm1hdDwvTmFtZT4KCQkJCTxPcGVyYXRpb25zPlJXPC9PcGVyYXRpb25zPgoJCQkJPE11bHRpcGxlSW5zdGFuY2VzPlNpbmdsZTwvTXVsdGlwbGVJbnN0YW5jZXM+CgkJCQk8TWFuZGF0b3J5Pk9wdGlvbmFsPC9NYW5kYXRvcnk+CgkJCQk8VHlwZT5TdHJpbmc8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbj4zMiBieXRlczwvUmFuZ2VFbnVtZXJhdGlvbj4KCQkJCTxVbml0cyAvPgoJCQkJPERlc2NyaXB0aW9uPjwhW0NEQVRBW0luZGljYXRlcyB0aGUgZm9ybWF0IG9mIHRoZSBBcHBsaWNhdGlvbiBEYXRhLgplLmcuIFlHLU1ldGVyLVdhdGVyLVJlYWRpbmcKVVRGOC1zdHJpbmcKXV0+PC9EZXNjcmlwdGlvbj4KCQkJPC9JdGVtPgoJCQk8SXRlbSBJRD0iNSI+PE5hbWU+QXBwIElEPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPkludGVnZXI8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbj4yIGJ5dGVzPC9SYW5nZUVudW1lcmF0aW9uPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBkZXN0aW5hdGlvbiBBcHBsaWNhdGlvbiBJRC5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+PC9SZXNvdXJjZXM+CgkJPERlc2NyaXB0aW9uMj48IVtDREFUQVtdXT48L0Rlc2NyaXB0aW9uMj4KCTwvT2JqZWN0Pgo8L0xXTTJNPgo=";

    private Tenant savedTenant;
    private User tenantAdmin;

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
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveTbResource() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(savedResource, savedResource.getId(), savedResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED);

        Assert.assertNotNull(savedResource);
        Assert.assertNotNull(savedResource.getId());
        Assert.assertTrue(savedResource.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedResource.getTenantId());
        Assert.assertEquals(resource.getTitle(), savedResource.getTitle());
        Assert.assertEquals(DEFAULT_FILE_NAME, savedResource.getFileName());
        Assert.assertEquals(DEFAULT_FILE_NAME, savedResource.getResourceKey());
        Assert.assertArrayEquals(resource.getData(), download(savedResource.getId()));

        String resourceTitle = "My new resource";
        savedResource = doPut("/api/resource/" + savedResource.getUuidId() + "/title", resourceTitle, TbResourceInfo.class);
        assertThat(savedResource.getTitle()).isEqualTo(resourceTitle);

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(savedResource, savedResource.getId(), savedResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED, ActionType.UPDATED);
    }

    @Test
    public void saveResourceInfoWithViolationOfLengthValidation() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle(StringUtils.randomAlphabetic(300));
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("title");
        doPost("/api/resource", resource)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(new TbResourceInfo(resource), savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateTbResourceFromDifferentTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/resource", savedResource)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedResource.getId(), savedResource);

        doDelete("/api/resource/" + savedResource.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedResource.getId(), savedResource);

        deleteDifferentTenant();
    }

    @Test
    public void testFindTbResourceById() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        TbResource foundResource = doGet("/api/resource/" + savedResource.getUuidId(), TbResource.class);
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedResource.getId(), foundResource.getId());
        Assert.assertEquals(savedResource.getFileName(), foundResource.getFileName());
    }

    @Test
    public void testFindSystemResourceInfoById() throws Exception {
        loginSysAdmin();
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My system resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);
        TbResourceInfo savedResourceInfo = save(resource);
        assertThat(savedResourceInfo.getFileName()).isEqualTo(DEFAULT_FILE_NAME);

        TbResourceInfo resourceInfo = findResourceInfo(savedResourceInfo.getId());
        assertThat(resourceInfo).isEqualTo(savedResourceInfo);
        loginTenantAdmin();
        resourceInfo = findResourceInfo(savedResourceInfo.getId());
        assertThat(resourceInfo).isEqualTo(savedResourceInfo);

        loginSysAdmin();
        resource = new TbResource(savedResourceInfo);
        resource.setFileName(DEFAULT_FILE_NAME_2);
        resource.setEncodedData(TEST_DATA);
        savedResourceInfo = save(resource);
        assertThat(savedResourceInfo.getFileName()).isEqualTo(DEFAULT_FILE_NAME_2);

        resourceInfo = findResourceInfo(savedResourceInfo.getId());
        assertThat(resourceInfo).isEqualTo(savedResourceInfo);
        loginTenantAdmin();
        resourceInfo = findResourceInfo(savedResourceInfo.getId());
        assertThat(resourceInfo).isEqualTo(savedResourceInfo);
    }

    @Test
    public void testFindTenantResourceInfoById() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My tenant resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);
        TbResourceInfo savedResourceInfo = save(resource);
        assertThat(savedResourceInfo.getFileName()).isEqualTo(DEFAULT_FILE_NAME);

        TbResourceInfo resourceInfo = findResourceInfo(savedResourceInfo.getId());
        assertThat(resourceInfo).isEqualTo(savedResourceInfo);

        resource = new TbResource(savedResourceInfo);
        resource.setFileName(DEFAULT_FILE_NAME_2);
        resource.setEncodedData(TEST_DATA);
        savedResourceInfo = save(resource);
        assertThat(savedResourceInfo.getFileName()).isEqualTo(DEFAULT_FILE_NAME_2);

        resourceInfo = findResourceInfo(savedResourceInfo.getId());
        assertThat(resourceInfo).isEqualTo(savedResourceInfo);
    }

    @Test
    public void testDeleteTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        Mockito.reset(tbClusterService, auditLogService);
        String resourceIdStr = savedResource.getId().getId().toString();
        doDelete("/api/resource/" + resourceIdStr)
                .andExpect(status().isOk());


        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(savedResource, savedResource.getId(), savedResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, ActionType.DELETED, resourceIdStr);

        doGet("/api/resource/" + savedResource.getUuidId())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Resource", resourceIdStr))));
    }

    @Test
    public void testUnForcedDeleteTbResourceIfAssignedToWidgetType() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My first resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setEncodedData(TEST_DATA);
        resource.setResourceKey(JS_TEST_FILE_NAME);

        TbResourceInfo savedResource = save(resource);

        var link = resource.getLink();
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setTenantId(savedTenant.getId());
        widgetType.setDescriptor(JacksonUtil.newObjectNode()
                .put("controllerScript", "self.onInit = function() {\n    self.ctx.$scope.actionWidget.onInit();\n}\n\nself.typeParameters = function() {\n    return {\n        previewWidth: '300px',\n        previewHeight: '320px',\n        embedTitlePanel: true,\n        targetDeviceOptional: true,\n        displayRpcMessageToast: false\n    };\n};\n\nself.onDestroy = function() {\n}")
                .put("settingsSchema", "")
                .put("dataKeySettingsSchema", "{}\n")
                .put("settingsDirective", "tb-scada-symbol-widget-settings")
                .put("hasBasicMode", true)
                .put("basicModeDirective", "tb-scada-symbol-basic-config")
                .put("resource", link));
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        var deleteResponse = doDelete("/api/resource/" + savedResource.getUuidId() + "?force=false")
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assert.assertNotNull(deleteResponse);

        boolean isSuccess = JacksonUtil.toJsonNode(deleteResponse).get("success").asBoolean();
        Assert.assertFalse(isSuccess);

        var referenceValues = JacksonUtil.toJsonNode(deleteResponse).get("references");
        Assert.assertNotNull(referenceValues);

        var widgetTypeInfos = JacksonUtil.readValue(referenceValues.toString(), new TypeReference<HashMap<String, List<EntityInfo>>>() {
        });
        Assert.assertNotNull(widgetTypeInfos);
        Assert.assertFalse(widgetTypeInfos.isEmpty());
        Assert.assertEquals(1, widgetTypeInfos.size());

        var widgetTypeInfo = widgetTypeInfos.get(EntityType.WIDGET_TYPE.name()).get(0);
        Assert.assertNotNull(widgetTypeInfo);
        Assert.assertEquals(new EntityInfo(savedWidgetType.getId(), savedWidgetType.getName()), widgetTypeInfo);
    }

    @Test
    public void testForcedDeleteTbResourceIfAssignedToWidgetType() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My first resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setEncodedData(TEST_DATA);
        resource.setResourceKey(JS_TEST_FILE_NAME);
        TbResourceInfo savedResource = save(resource);

        var link = resource.getLink();
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setTenantId(savedTenant.getId());
        widgetType.setDescriptor(JacksonUtil.newObjectNode()
                .put("controllerScript", "self.onInit = function() {\n    self.ctx.$scope.actionWidget.onInit();\n}\n\nself.typeParameters = function() {\n    return {\n        previewWidth: '300px',\n        previewHeight: '320px',\n        embedTitlePanel: true,\n        targetDeviceOptional: true,\n        displayRpcMessageToast: false\n    };\n};\n\nself.onDestroy = function() {\n}")
                .put("settingsSchema", "")
                .put("dataKeySettingsSchema", "{}\n")
                .put("settingsDirective", "tb-scada-symbol-widget-settings")
                .put("hasBasicMode", true)
                .put("basicModeDirective", "tb-scada-symbol-basic-config")
                .put("resource", link));
        doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        var deleteResponse = doDelete("/api/resource/" + savedResource.getUuidId() + "?force=true")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assert.assertNotNull(deleteResponse);

        boolean isSuccess = JacksonUtil.toJsonNode(deleteResponse).get("success").asBoolean();
        Assert.assertTrue(isSuccess);

        var referenceValues = JacksonUtil.toJsonNode(deleteResponse).get("references");
        var widgetTypeInfos = JacksonUtil.readValue(referenceValues.toString(), new TypeReference<HashMap<String, List<EntityInfo>>>() {
        });
        Assert.assertNull(widgetTypeInfos);
    }

    @Test
    public void testUnForcedDeleteTbResourceIfAssignedToDashboard() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My first resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setEncodedData(TEST_DATA);
        resource.setResourceKey(JS_TEST_FILE_NAME);
        TbResourceInfo savedResource = save(resource);

        var link = resource.getLink();
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(savedTenant.getId());
        dashboard.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("widgets", JacksonUtil.toJsonNode("""
                        {"xxx":
                        {"config":{"actions":{"elementClick":[
                        {"customResources":[{"url":{"entityType":"TB_RESOURCE","id":
                        "tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js"},"isModule":true},
                        {"url":"tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js","isModule":true}]}]}}}}
                        """))
                .put("resource", link));
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        var deleteResponse = doDelete("/api/resource/" + savedResource.getUuidId() + "?force=false")
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assert.assertNotNull(deleteResponse);

        boolean isSuccess = JacksonUtil.toJsonNode(deleteResponse).get("success").asBoolean();
        Assert.assertFalse(isSuccess);

        var referenceValues = JacksonUtil.toJsonNode(deleteResponse).get("references");
        Assert.assertNotNull(referenceValues);

        var dashboardInfos = JacksonUtil.readValue(referenceValues.toString(), new TypeReference<HashMap<String, List<EntityInfo>>>() {
        });
        Assert.assertNotNull(dashboardInfos);
        Assert.assertFalse(dashboardInfos.isEmpty());
        Assert.assertEquals(1, dashboardInfos.size());

        var dashboardInfo = dashboardInfos.get(EntityType.DASHBOARD.name()).get(0);
        Assert.assertNotNull(dashboardInfo);
        Assert.assertEquals(new EntityInfo(savedDashboard.getId(), savedDashboard.getName()), dashboardInfo);
    }

    @Test
    public void testForcedDeleteTbResourceIfAssignedToDashboard() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My first resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setEncodedData(TEST_DATA);
        resource.setResourceKey(JS_TEST_FILE_NAME);
        TbResourceInfo savedResource = save(resource);

        var link = resource.getLink();
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(savedTenant.getId());
        dashboard.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("widgets", JacksonUtil.toJsonNode("""
                        {"xxx":
                        {"config":{"actions":{"elementClick":[
                        {"customResources":[{"url":{"entityType":"TB_RESOURCE","id":
                        "tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js"},"isModule":true},
                        {"url":"tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js","isModule":true}]}]}}}}
                        """))
                .put("resource", link));
        doPost("/api/dashboard", dashboard, Dashboard.class);

        var deleteResponse = doDelete("/api/resource/" + savedResource.getUuidId() + "?force=true")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assert.assertNotNull(deleteResponse);

        boolean isSuccess = JacksonUtil.toJsonNode(deleteResponse).get("success").asBoolean();
        Assert.assertTrue(isSuccess);

        var referenceValues = JacksonUtil.toJsonNode(deleteResponse).get("references");
        var dashboardInfos = JacksonUtil.readValue(referenceValues.toString(), new TypeReference<HashMap<String, List<EntityInfo>>>() {
        });
        Assert.assertNull(dashboardInfos);
    }

    @Test
    public void testFindTenantTbResources() throws Exception {

        Mockito.reset(tbClusterService, auditLogService);

        List<TbResourceInfo> resources = new ArrayList<>();
        int cntEntity = 173;
        for (int i = 0; i < cntEntity; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setEncodedData(TEST_DATA);
            resources.add(new TbResourceInfo(save(resource)));
        }
        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new TbResourceInfo(), new TbResourceInfo(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        resources.sort(idComparator);
        loadedResources.sort(idComparator);

        Assert.assertEquals(resources, loadedResources);
    }

    @Test
    public void testFindTenantTbResourcesByType() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);

        List<TbResourceInfo> resources = new ArrayList<>();
        int jksCntEntity = 17;
        for (int i = 0; i < jksCntEntity; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("JKS Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setEncodedData(TEST_DATA);
            resources.add(new TbResourceInfo(save(resource)));
        }

        int lwm2mCntEntity = 19;
        for (int i = 0; i < lwm2mCntEntity; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("LWM2M Resource" + i);
            resource.setResourceType(ResourceType.PKCS_12);
            resource.setFileName(i + DEFAULT_FILE_NAME_2);
            resource.setEncodedData(TEST_DATA);
            save(resource);
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(5);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?resourceType=" + ResourceType.JKS.name() + "&",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new TbResourceInfo(), new TbResourceInfo(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                jksCntEntity + lwm2mCntEntity, jksCntEntity + lwm2mCntEntity, jksCntEntity + lwm2mCntEntity);

        resources.sort(idComparator);
        loadedResources.sort(idComparator);

        Assert.assertEquals(resources, loadedResources);
    }

    @Test
    public void testFindSystemTbResources() throws Exception {
        loginSysAdmin();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 173; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setEncodedData(TEST_DATA);
            resources.add(new TbResourceInfo(save(resource)));
        }
        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        resources.sort(idComparator);
        loadedResources.sort(idComparator);

        Assert.assertEquals(resources, loadedResources);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = resources.size();
        for (TbResourceInfo resource : resources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new TbResource(), new TbResource(),
                resources.get(0).getTenantId(), null, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, 1);

        pageLink = new PageLink(27);
        loadedResources.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(loadedResources.isEmpty());
    }

    @Test
    public void testFindSystemTbResourcesByType() throws Exception {
        loginSysAdmin();

        List<TbResourceInfo> jksResources = new ArrayList<>();
        List<TbResourceInfo> lwm2mesources = new ArrayList<>();
        int jksCntEntity = 17;
        for (int i = 0; i < jksCntEntity; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("JKS Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setEncodedData(TEST_DATA);
            TbResourceInfo saved = new TbResourceInfo(save(resource));
            jksResources.add(saved);
        }

        int lwm2mCntEntity = 19;
        for (int i = 0; i < lwm2mCntEntity; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("LWM2M Resource" + i);
            resource.setResourceType(ResourceType.PKCS_12);
            resource.setFileName(i + DEFAULT_FILE_NAME_2);
            resource.setEncodedData(TEST_DATA);
            TbResourceInfo saved = save(resource);
            lwm2mesources.add(saved);
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(30);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?resourceType=" + ResourceType.JKS + "&",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        jksResources.sort(idComparator);
        loadedResources.sort(idComparator);

        Assert.assertEquals(jksResources, loadedResources);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = jksResources.size();
        for (TbResourceInfo resource : jksResources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new TbResource(), new TbResource(),
                jksResources.get(0).getTenantId(), null, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, 1);

        pageLink = new PageLink(27);
        loadedResources.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/resource?resourceType=" + ResourceType.JKS + "&",
                    new TypeReference<>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(loadedResources.isEmpty());

        loginSysAdmin();

        for (TbResourceInfo resource : lwm2mesources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testFindSystemAndTenantTbResources() throws Exception {
        List<TbResourceInfo> systemResources = new ArrayList<>();
        List<TbResourceInfo> expectedResources = new ArrayList<>();
        for (int i = 0; i < 73; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setEncodedData(TEST_DATA);
            expectedResources.add(new TbResourceInfo(save(resource)));
        }

        loginSysAdmin();

        for (int i = 0; i < 173; i++) {
            TbResource resource = new TbResource();
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setEncodedData(TEST_DATA);
            TbResourceInfo savedResource = new TbResourceInfo(save(resource));
            systemResources.add(savedResource);
            if (i >= 73) {
                expectedResources.add(savedResource);
            }
        }

        login(tenantAdmin.getEmail(), "testPassword1");

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(24);
        PageData<TbResourceInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/resource?",
                    new TypeReference<PageData<TbResourceInfo>>() {
                    }, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        expectedResources.sort(idComparator);
        loadedResources.sort(idComparator);

        Assert.assertEquals(expectedResources, loadedResources);

        loginSysAdmin();

        for (TbResourceInfo resource : systemResources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testDownloadTbResourceIfChanged() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);

        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("Js resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(savedResource, savedResource.getId(), savedResource.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED);

        ResultActions resultActions = doGet("/api/resource/js/" + savedResource.getId().getId().toString() + "/download")
                .andExpect(status().isOk());
        MockHttpServletResponse response = resultActions.andReturn().getResponse();
        String eTag = response.getHeader("ETag");
        Assert.assertNotNull(eTag);
        Assert.assertEquals(TEST_DATA, Base64.getEncoder().encodeToString(response.getContentAsByteArray()));

        //download with if-none-match header
        HttpHeaders headers = new HttpHeaders();
        headers.setIfNoneMatch(eTag);
        doGet("/api/resource/js/" + savedResource.getId().getId().toString() + "/download", headers)
                .andExpect(status().isNotModified());
    }

    @Test
    public void testDownloadTbResourceIfChangedAsPublicCustomer() throws Exception {
        loginTenantAdmin();
        Mockito.reset(tbClusterService, auditLogService);

        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("Js resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        //download as public customer
        Device device = new Device();
        device.setName("Test Public Device");
        device.setLabel("Label");
        device.setCustomerId(customerId);
        device = doPost("/api/device", device, Device.class);
        device = doPost("/api/customer/public/device/" + device.getUuidId(), Device.class);

        String publicId = device.getCustomerId().toString();

        Mockito.reset(tbClusterService, auditLogService);
        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        ResultActions resultActions = doGet("/api/resource/js/" + savedResource.getId().getId().toString() + "/download")
                .andExpect(status().isOk());
        MockHttpServletResponse response = resultActions.andReturn().getResponse();
        String eTag = response.getHeader("ETag");
        Assert.assertNotNull(eTag);
        Assert.assertEquals(TEST_DATA, Base64.getEncoder().encodeToString(response.getContentAsByteArray()));

        //download with if-none-match header
        HttpHeaders headers = new HttpHeaders();
        headers.setIfNoneMatch(eTag);
        doGet("/api/resource/js/" + savedResource.getId().getId().toString() + "/download", headers)
                .andExpect(status().isNotModified());
    }

    @Test
    public void testDownloadTbResourceIfChangedAsCustomerOfDifferentTenant() throws Exception {
        loginTenantAdmin();
        Mockito.reset(tbClusterService, auditLogService);

        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("Js resource");
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        TbResourceInfo savedResource = save(resource);

        loginDifferentTenant();
        loginDifferentTenantCustomer();
        doGet("/api/resource/js/" + savedResource.getId().getId().toString() + "/download")
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateResourceData_nonUpdatableResourceType() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.PKCS_12);
        resource.setTitle("My resource");
        resource.setFileName("3.pks");
        resource.setEncodedData(TEST_DATA);
        TbResourceInfo savedResource = save(resource);
        resource.setEtag(savedResource.getEtag());

        TbResource foundResource = doGet("/api/resource/" + savedResource.getUuidId(), TbResource.class);

        foundResource.setEncodedData(TEST_DATA);
        doPost("/api/resource", foundResource)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("can't be updated")));

        String resourceTitle = "Updated resource";
        savedResource = doPut("/api/resource/" + savedResource.getUuidId() + "/title", resourceTitle, TbResourceInfo.class);
        assertThat(savedResource.getTitle()).isEqualTo(resourceTitle);
        assertThat(savedResource.getFileName()).isEqualTo(resource.getFileName());
        assertThat(savedResource.getEtag()).isEqualTo(resource.getEtag());
        assertThat(download(savedResource.getId())).asBase64Encoded().isEqualTo(TEST_DATA);
    }

    @Test
    public void testUpdateResourceData_updatableResourceType() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My resource");
        resource.setFileName("module.js");
        resource.setEncodedData(TEST_DATA);
        TbResourceInfo savedResource = save(resource);

        TbResource foundResource = doGet("/api/resource/" + savedResource.getUuidId(), TbResource.class);
        resource.setEtag(foundResource.getEtag());

        String newData = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        foundResource.setEncodedData(newData);
        foundResource.setFileName("new-module.js");
        foundResource.setTitle("Updated title");
        savedResource = save(foundResource);

        assertThat(savedResource.getTitle()).isEqualTo("Updated title");
        assertThat(savedResource.getFileName()).isEqualTo("new-module.js");
        assertThat(savedResource.getEtag()).isNotEqualTo(resource.getEtag());
        assertThat(download(savedResource.getId())).asBase64Encoded().isEqualTo(newData);
    }

    @Test
    public void testGetLwm2mListObjectsPage() throws Exception {
        loginTenantAdmin();

        List<TbResourceInfo> resources = loadLwm2mResources();

        List<LwM2mObject> objects =
                doGetTyped("/api/resource/lwm2m/page?pageSize=100&page=0", new TypeReference<>() {
                });
        Assert.assertNotNull(objects);
        Assert.assertEquals(resources.size(), objects.size());

        removeLoadResources(resources);
    }

    @Test
    public void testGetLwm2mListObjects() throws Exception {
        loginTenantAdmin();

        List<TbResourceInfo> resources = loadLwm2mResources();

        List<LwM2mObject> objects =
                doGetTyped("/api/resource/lwm2m?sortProperty=id&sortOrder=ASC&objectIds=3_1.2,5_1.2,19_1.1", new TypeReference<>() {
                });
        Assert.assertNotNull(objects);
        Assert.assertEquals(3, objects.size());

        removeLoadResources(resources);
    }

    private TbResourceInfo save(TbResource tbResource) throws Exception {
        byte[] data = tbResource.getData() != null ? tbResource.getData() : tbResource.getEncodedData() != null ? Base64.getDecoder().decode(tbResource.getEncodedData()) : null;
        List<MockPart> parts = new ArrayList<>();
        parts.add(new MockPart("resourceType", tbResource.getResourceType().name().getBytes()));

        if (tbResource.getId() != null) {
            parts.add(new MockPart("resourceId", tbResource.getId().getId().toString().getBytes()));
        }
        if (tbResource.getTitle() != null) {
            parts.add(new MockPart("title", tbResource.getTitle().getBytes()));
        }
        if (tbResource.getDescriptor() != null) {
            parts.add(new MockPart("descriptor", tbResource.getDescriptor().toString().getBytes()));
        }
        if (tbResource.getSearchText() != null) {
            parts.add(new MockPart("searchText", tbResource.getSearchText().getBytes()));
        }

        return uploadResource(HttpMethod.POST, "/api/resource/upload", tbResource.getFileName(), tbResource.getResourceType().getMediaType(), data, parts);
    }

    private TbResourceInfo findResourceInfo(TbResourceId id) throws Exception {
        return doGet("/api/resource/info/" + id, TbResourceInfo.class);
    }

    private byte[] download(TbResourceId resourceId) throws Exception {
        return doGet("/api/resource/" + resourceId + "/download")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String getImageLink(TbResourceInfo resourceInfo) {
        return "/api/images/" + (resourceInfo.getTenantId().isSysTenantId() ? "system/" : "") + resourceInfo.getResourceKey();
    }


    private List<TbResourceInfo> loadLwm2mResources() throws Exception {
        var models = List.of("1", "2", "3", "5", "6", "9", "19", "3303");

        List<TbResourceInfo> resources = new ArrayList<>(models.size());

        for (String model : models) {
            String fileName = model + ".xml";
            byte[] bytes = IOUtils.toByteArray(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("lwm2m/" + fileName)));

            TbResource resource = new TbResource();
            resource.setResourceType(ResourceType.LWM2M_MODEL);
            resource.setFileName(fileName);
            resource.setData(bytes);

            resources.add(save(resource));
        }
        return resources;
    }

    private void removeLoadResources(List<TbResourceInfo> resources) throws Exception {
        for (TbResourceInfo resource : resources) {
            doDelete("/api/resource/" + resource.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

}
