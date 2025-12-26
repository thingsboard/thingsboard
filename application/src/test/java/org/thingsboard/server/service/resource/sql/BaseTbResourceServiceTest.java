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
package org.thingsboard.server.service.resource.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.ai.TbAiNode;
import org.thingsboard.rule.engine.ai.TbAiNodeConfiguration;
import org.thingsboard.rule.engine.ai.TbResponseFormat;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDeleteResult;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.service.resource.TbResourceService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@DaoSqlTest
public class BaseTbResourceServiceTest extends AbstractControllerTest {

    private static final String LWM2M_TEST_MODEL = "<LWM2M xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.openmobilealliance.org/tech/profiles/LWM2M-v1_1.xsd\">\n" +
            "<Object ObjectType=\"MODefinition\">\n" +
            "<Name>My first resource</Name>\n" +
            "<Description1></Description1>\n" +
            "<ObjectID>0</ObjectID>\n" +
            "<ObjectURN></ObjectURN>\n" +
            "<ObjectVersion>1.0</ObjectVersion>\n" +
            "<MultipleInstances>Multiple</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Resources>\n" +
            "<Item ID=\"0\">\n" +
            "<Name>LWM2M</Name>\n" +
            "<Operations>RW</Operations>\n" +
            "<MultipleInstances>Single</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Type>String</Type>\n" +
            "<RangeEnumeration>0..255</RangeEnumeration>\n" +
            "<Units></Units>\n" +
            "<Description></Description>\n" +
            "</Item>\n" +
            "</Resources>\n" +
            "<Description2></Description2>\n" +
            "</Object>\n" +
            "</LWM2M>";

    private static final String LWM2M_TEST_MODEL_WITH_XXE = "<!DOCTYPE replace [<!ENTITY ObjectVersion SYSTEM \"file:///etc/hostname\"> ]>" +
            "<LWM2M xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.openmobilealliance.org/tech/profiles/LWM2M-v1_1.xsd\">\n" +
            "<Object ObjectType=\"MODefinition\">\n" +
            "<Name>My first resource</Name>\n" +
            "<Description1></Description1>\n" +
            "<ObjectID>0</ObjectID>\n" +
            "<ObjectURN></ObjectURN>\n" +
            "<ObjectVersion>&ObjectVersion;</ObjectVersion>\n" +
            "<MultipleInstances>Multiple</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Resources>\n" +
            "<Item ID=\"0\">\n" +
            "<Name>LWM2M</Name>\n" +
            "<Operations>RW</Operations>\n" +
            "<MultipleInstances>Single</MultipleInstances>\n" +
            "<Mandatory>Mandatory</Mandatory>\n" +
            "<Type>String</Type>\n" +
            "<RangeEnumeration>0..255</RangeEnumeration>\n" +
            "<Units></Units>\n" +
            "<Description></Description>\n" +
            "</Item>\n" +
            "</Resources>\n" +
            "<Description2></Description2>\n" +
            "</Object>\n" +
            "</LWM2M>";

    private static final String DEFAULT_FILE_NAME = "test.jks";
    private static final String JS_FILE_NAME = "test.js";
    private static final String TEST_BASE64_DATA = "77u/PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPCEtLQpGSUxFIElORk9STUFUSU9OCgpPTUEgUGVybWFuZW50IERvY3VtZW50CiAgIEZpbGU6IE9NQS1TVVAtTHdNMk1fQmluYXJ5QXBwRGF0YUNvbnRhaW5lci1WMV8wXzEtMjAxOTAyMjEtQQogICBUeXBlOiB4bWwKClB1YmxpYyBSZWFjaGFibGUgSW5mb3JtYXRpb24KICAgUGF0aDogaHR0cDovL3d3dy5vcGVubW9iaWxlYWxsaWFuY2Uub3JnL3RlY2gvcHJvZmlsZXMKICAgTmFtZTogTHdNMk1fQmluYXJ5QXBwRGF0YUNvbnRhaW5lci12MV8wXzEueG1sCgpOT1JNQVRJVkUgSU5GT1JNQVRJT04KCiAgSW5mb3JtYXRpb24gYWJvdXQgdGhpcyBmaWxlIGNhbiBiZSBmb3VuZCBpbiB0aGUgbGF0ZXN0IHJldmlzaW9uIG9mCgogIE9NQS1UUy1MV00yTV9CaW5hcnlBcHBEYXRhQ29udGFpbmVyLVYxXzBfMQoKICBUaGlzIGlzIGF2YWlsYWJsZSBhdCBodHRwOi8vd3d3Lm9wZW5tb2JpbGVhbGxpYW5jZS5vcmcvCgogIFNlbmQgY29tbWVudHMgdG8gaHR0cHM6Ly9naXRodWIuY29tL09wZW5Nb2JpbGVBbGxpYW5jZS9PTUFfTHdNMk1fZm9yX0RldmVsb3BlcnMvaXNzdWVzCgpDSEFOR0UgSElTVE9SWQoKMTUwNjIwMTggU3RhdHVzIGNoYW5nZWQgdG8gQXBwcm92ZWQgYnkgRE0sIERvYyBSZWYgIyBPTUEtRE0mU0UtMjAxOC0wMDYxLUlOUF9MV00yTV9BUFBEQVRBX1YxXzBfRVJQX2Zvcl9maW5hbF9BcHByb3ZhbAoyMTAyMjAxOSBTdGF0dXMgY2hhbmdlZCB0byBBcHByb3ZlZCBieSBJUFNPLCBEb2MgUmVmICMgT01BLUlQU08tMjAxOS0wMDI1LUlOUF9Md00yTV9PYmplY3RfQXBwX0RhdGFfQ29udGFpbmVyXzFfMF8xX2Zvcl9GaW5hbF9BcHByb3ZhbAoKTEVHQUwgRElTQ0xBSU1FUgoKQ29weXJpZ2h0IDIwMTkgT3BlbiBNb2JpbGUgQWxsaWFuY2UuCgpSZWRpc3RyaWJ1dGlvbiBhbmQgdXNlIGluIHNvdXJjZSBhbmQgYmluYXJ5IGZvcm1zLCB3aXRoIG9yIHdpdGhvdXQKbW9kaWZpY2F0aW9uLCBhcmUgcGVybWl0dGVkIHByb3ZpZGVkIHRoYXQgdGhlIGZvbGxvd2luZyBjb25kaXRpb25zCmFyZSBtZXQ6CgoxLiBSZWRpc3RyaWJ1dGlvbnMgb2Ygc291cmNlIGNvZGUgbXVzdCByZXRhaW4gdGhlIGFib3ZlIGNvcHlyaWdodApub3RpY2UsIHRoaXMgbGlzdCBvZiBjb25kaXRpb25zIGFuZCB0aGUgZm9sbG93aW5nIGRpc2NsYWltZXIuCjIuIFJlZGlzdHJpYnV0aW9ucyBpbiBiaW5hcnkgZm9ybSBtdXN0IHJlcHJvZHVjZSB0aGUgYWJvdmUgY29weXJpZ2h0Cm5vdGljZSwgdGhpcyBsaXN0IG9mIGNvbmRpdGlvbnMgYW5kIHRoZSBmb2xsb3dpbmcgZGlzY2xhaW1lciBpbiB0aGUKZG9jdW1lbnRhdGlvbiBhbmQvb3Igb3RoZXIgbWF0ZXJpYWxzIHByb3ZpZGVkIHdpdGggdGhlIGRpc3RyaWJ1dGlvbi4KMy4gTmVpdGhlciB0aGUgbmFtZSBvZiB0aGUgY29weXJpZ2h0IGhvbGRlciBub3IgdGhlIG5hbWVzIG9mIGl0cwpjb250cmlidXRvcnMgbWF5IGJlIHVzZWQgdG8gZW5kb3JzZSBvciBwcm9tb3RlIHByb2R1Y3RzIGRlcml2ZWQKZnJvbSB0aGlzIHNvZnR3YXJlIHdpdGhvdXQgc3BlY2lmaWMgcHJpb3Igd3JpdHRlbiBwZXJtaXNzaW9uLgoKVEhJUyBTT0ZUV0FSRSBJUyBQUk9WSURFRCBCWSBUSEUgQ09QWVJJR0hUIEhPTERFUlMgQU5EIENPTlRSSUJVVE9SUwoiQVMgSVMiIEFORCBBTlkgRVhQUkVTUyBPUiBJTVBMSUVEIFdBUlJBTlRJRVMsIElOQ0xVRElORywgQlVUIE5PVApMSU1JVEVEIFRPLCBUSEUgSU1QTElFRCBXQVJSQU5USUVTIE9GIE1FUkNIQU5UQUJJTElUWSBBTkQgRklUTkVTUwpGT1IgQSBQQVJUSUNVTEFSIFBVUlBPU0UgQVJFIERJU0NMQUlNRUQuIElOIE5PIEVWRU5UIFNIQUxMIFRIRQpDT1BZUklHSFQgSE9MREVSIE9SIENPTlRSSUJVVE9SUyBCRSBMSUFCTEUgRk9SIEFOWSBESVJFQ1QsIElORElSRUNULApJTkNJREVOVEFMLCBTUEVDSUFMLCBFWEVNUExBUlksIE9SIENPTlNFUVVFTlRJQUwgREFNQUdFUyAoSU5DTFVESU5HLApCVVQgTk9UIExJTUlURUQgVE8sIFBST0NVUkVNRU5UIE9GIFNVQlNUSVRVVEUgR09PRFMgT1IgU0VSVklDRVM7CkxPU1MgT0YgVVNFLCBEQVRBLCBPUiBQUk9GSVRTOyBPUiBCVVNJTkVTUyBJTlRFUlJVUFRJT04pIEhPV0VWRVIKQ0FVU0VEIEFORCBPTiBBTlkgVEhFT1JZIE9GIExJQUJJTElUWSwgV0hFVEhFUiBJTiBDT05UUkFDVCwgU1RSSUNUCkxJQUJJTElUWSwgT1IgVE9SVCAoSU5DTFVESU5HIE5FR0xJR0VOQ0UgT1IgT1RIRVJXSVNFKSBBUklTSU5HIElOCkFOWSBXQVkgT1VUIE9GIFRIRSBVU0UgT0YgVEhJUyBTT0ZUV0FSRSwgRVZFTiBJRiBBRFZJU0VEIE9GIFRIRQpQT1NTSUJJTElUWSBPRiBTVUNIIERBTUFHRS4KClRoZSBhYm92ZSBsaWNlbnNlIGlzIHVzZWQgYXMgYSBsaWNlbnNlIHVuZGVyIGNvcHlyaWdodCBvbmx5LiBQbGVhc2UKcmVmZXJlbmNlIHRoZSBPTUEgSVBSIFBvbGljeSBmb3IgcGF0ZW50IGxpY2Vuc2luZyB0ZXJtczoKaHR0cHM6Ly93d3cub21hc3BlY3dvcmtzLm9yZy9hYm91dC9pbnRlbGxlY3R1YWwtcHJvcGVydHktcmlnaHRzLwoKLS0+CjxMV00yTSB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIiB4c2k6bm9OYW1lc3BhY2VTY2hlbWFMb2NhdGlvbj0iaHR0cDovL29wZW5tb2JpbGVhbGxpYW5jZS5vcmcvdGVjaC9wcm9maWxlcy9MV00yTS54c2QiPgoJPE9iamVjdCBPYmplY3RUeXBlPSJNT0RlZmluaXRpb24iPgoJCTxOYW1lPkJpbmFyeUFwcERhdGFDb250YWluZXI8L05hbWU+CgkJPERlc2NyaXB0aW9uMT48IVtDREFUQVtUaGlzIEx3TTJNIE9iamVjdHMgcHJvdmlkZXMgdGhlIGFwcGxpY2F0aW9uIHNlcnZpY2UgZGF0YSByZWxhdGVkIHRvIGEgTHdNMk0gU2VydmVyLCBlZy4gV2F0ZXIgbWV0ZXIgZGF0YS4gClRoZXJlIGFyZSBzZXZlcmFsIG1ldGhvZHMgdG8gY3JlYXRlIGluc3RhbmNlIHRvIGluZGljYXRlIHRoZSBtZXNzYWdlIGRpcmVjdGlvbiBiYXNlZCBvbiB0aGUgbmVnb3RpYXRpb24gYmV0d2VlbiBBcHBsaWNhdGlvbiBhbmQgTHdNMk0uIFRoZSBDbGllbnQgYW5kIFNlcnZlciBzaG91bGQgbmVnb3RpYXRlIHRoZSBpbnN0YW5jZShzKSB1c2VkIHRvIGV4Y2hhbmdlIHRoZSBkYXRhLiBGb3IgZXhhbXBsZToKIC0gVXNpbmcgYSBzaW5nbGUgaW5zdGFuY2UgZm9yIGJvdGggZGlyZWN0aW9ucyBjb21tdW5pY2F0aW9uLCBmcm9tIENsaWVudCB0byBTZXJ2ZXIgYW5kIGZyb20gU2VydmVyIHRvIENsaWVudC4KIC0gVXNpbmcgYW4gaW5zdGFuY2UgZm9yIGNvbW11bmljYXRpb24gZnJvbSBDbGllbnQgdG8gU2VydmVyIGFuZCBhbm90aGVyIG9uZSBmb3IgY29tbXVuaWNhdGlvbiBmcm9tIFNlcnZlciB0byBDbGllbnQKIC0gVXNpbmcgc2V2ZXJhbCBpbnN0YW5jZXMKXV0+PC9EZXNjcmlwdGlvbjE+CgkJPE9iamVjdElEPjE5PC9PYmplY3RJRD4KCQk8T2JqZWN0VVJOPnVybjpvbWE6bHdtMm06b21hOjE5PC9PYmplY3RVUk4+CgkJPExXTTJNVmVyc2lvbj4xLjA8L0xXTTJNVmVyc2lvbj4KCQk8T2JqZWN0VmVyc2lvbj4xLjA8L09iamVjdFZlcnNpb24+CgkJPE11bHRpcGxlSW5zdGFuY2VzPk11bHRpcGxlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQk8TWFuZGF0b3J5Pk9wdGlvbmFsPC9NYW5kYXRvcnk+CgkJPFJlc291cmNlcz4KCQkJPEl0ZW0gSUQ9IjAiPjxOYW1lPkRhdGE8L05hbWU+CgkJCQk8T3BlcmF0aW9ucz5SVzwvT3BlcmF0aW9ucz4KCQkJCTxNdWx0aXBsZUluc3RhbmNlcz5NdWx0aXBsZTwvTXVsdGlwbGVJbnN0YW5jZXM+CgkJCQk8TWFuZGF0b3J5Pk1hbmRhdG9yeTwvTWFuZGF0b3J5PgoJCQkJPFR5cGU+T3BhcXVlPC9UeXBlPgoJCQkJPFJhbmdlRW51bWVyYXRpb24gLz4KCQkJCTxVbml0cyAvPgoJCQkJPERlc2NyaXB0aW9uPjwhW0NEQVRBW0luZGljYXRlcyB0aGUgYXBwbGljYXRpb24gZGF0YSBjb250ZW50Ll1dPjwvRGVzY3JpcHRpb24+CgkJCTwvSXRlbT4KCQkJPEl0ZW0gSUQ9IjEiPjxOYW1lPkRhdGEgUHJpb3JpdHk8L05hbWU+CgkJCQk8T3BlcmF0aW9ucz5SVzwvT3BlcmF0aW9ucz4KCQkJCTxNdWx0aXBsZUluc3RhbmNlcz5TaW5nbGU8L011bHRpcGxlSW5zdGFuY2VzPgoJCQkJPE1hbmRhdG9yeT5PcHRpb25hbDwvTWFuZGF0b3J5PgoJCQkJPFR5cGU+SW50ZWdlcjwvVHlwZT4KCQkJCTxSYW5nZUVudW1lcmF0aW9uPjEgYnl0ZXM8L1JhbmdlRW51bWVyYXRpb24+CgkJCQk8VW5pdHMgLz4KCQkJCTxEZXNjcmlwdGlvbj48IVtDREFUQVtJbmRpY2F0ZXMgdGhlIEFwcGxpY2F0aW9uIGRhdGEgcHJpb3JpdHk6CjA6SW1tZWRpYXRlCjE6QmVzdEVmZm9ydAoyOkxhdGVzdAozLTEwMDogUmVzZXJ2ZWQgZm9yIGZ1dHVyZSB1c2UuCjEwMS0yNTQ6IFByb3ByaWV0YXJ5IG1vZGUuXV0+PC9EZXNjcmlwdGlvbj4KCQkJPC9JdGVtPgoJCQk8SXRlbSBJRD0iMiI+PE5hbWU+RGF0YSBDcmVhdGlvbiBUaW1lPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPlRpbWU8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbiAvPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBEYXRhIGluc3RhbmNlIGNyZWF0aW9uIHRpbWVzdGFtcC5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+CgkJCTxJdGVtIElEPSIzIj48TmFtZT5EYXRhIERlc2NyaXB0aW9uPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPlN0cmluZzwvVHlwZT4KCQkJCTxSYW5nZUVudW1lcmF0aW9uPjMyIGJ5dGVzPC9SYW5nZUVudW1lcmF0aW9uPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBkYXRhIGRlc2NyaXB0aW9uLgplLmcuICJtZXRlciByZWFkaW5nIi5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+CgkJCTxJdGVtIElEPSI0Ij48TmFtZT5EYXRhIEZvcm1hdDwvTmFtZT4KCQkJCTxPcGVyYXRpb25zPlJXPC9PcGVyYXRpb25zPgoJCQkJPE11bHRpcGxlSW5zdGFuY2VzPlNpbmdsZTwvTXVsdGlwbGVJbnN0YW5jZXM+CgkJCQk8TWFuZGF0b3J5Pk9wdGlvbmFsPC9NYW5kYXRvcnk+CgkJCQk8VHlwZT5TdHJpbmc8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbj4zMiBieXRlczwvUmFuZ2VFbnVtZXJhdGlvbj4KCQkJCTxVbml0cyAvPgoJCQkJPERlc2NyaXB0aW9uPjwhW0NEQVRBW0luZGljYXRlcyB0aGUgZm9ybWF0IG9mIHRoZSBBcHBsaWNhdGlvbiBEYXRhLgplLmcuIFlHLU1ldGVyLVdhdGVyLVJlYWRpbmcKVVRGOC1zdHJpbmcKXV0+PC9EZXNjcmlwdGlvbj4KCQkJPC9JdGVtPgoJCQk8SXRlbSBJRD0iNSI+PE5hbWU+QXBwIElEPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPkludGVnZXI8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbj4yIGJ5dGVzPC9SYW5nZUVudW1lcmF0aW9uPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBkZXN0aW5hdGlvbiBBcHBsaWNhdGlvbiBJRC5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+PC9SZXNvdXJjZXM+CgkJPERlc2NyaXB0aW9uMj48IVtDREFUQVtdXT48L0Rlc2NyaXB0aW9uMj4KCTwvT2JqZWN0Pgo8L0xXTTJNPgo=";
    private static final byte[] TEST_DATA = Base64.getDecoder().decode(TEST_BASE64_DATA);

    private IdComparator<TbResourceInfo> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Autowired
    private ResourceService resourceService;
    @Autowired
    private TbResourceService tbResourceService;
    @Autowired
    private WidgetTypeService widgetTypeService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private AiModelService aiModelService;

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        tenantId = savedTenant.getId();
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

        resourceService.deleteResourcesByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testSaveResourceWithMaxSumDataSizeOutOfLimit() throws Exception {
        loginSysAdmin();
        long limit = 4;
        EntityInfo defaultTenantProfileInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        TenantProfile defaultTenantProfile = doGet("/api/tenantProfile/" + defaultTenantProfileInfo.getId().getId().toString(), TenantProfile.class);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder()
                .maxResourcesInBytes(limit).build());
        doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);

        loginTenantAdmin();

        assertEquals(0, resourceService.sumDataSizeByTenantId(tenantId));

        createResource("test", DEFAULT_FILE_NAME);

        assertEquals(4, resourceService.sumDataSizeByTenantId(tenantId));

        try {
            assertThatThrownBy(() -> createResource("test1", 1 + DEFAULT_FILE_NAME))
                    .isInstanceOf(DataValidationException.class)
                    .hasMessageContaining("Resources total size exceeds the maximum of %s bytes", limit);
        } finally {
            defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxResourcesInBytes(0).build());
            loginSysAdmin();
            doPost("/api/tenantProfile", defaultTenantProfile, TenantProfile.class);
        }
    }

    @Test
    public void testMaxResourceSizeValidation() throws ThingsboardException {
        updateDefaultTenantProfileConfig(profileConfig -> {
            profileConfig.setMaxResourceSize(2);
        });
        assertThatThrownBy(() -> createResource("Test", DEFAULT_FILE_NAME))
                .hasMessageContaining("Resource exceeds the maximum size of 2 bytes");
    }

    @Test
    public void sumDataSizeByTenantId() throws Exception {
        assertEquals(0, resourceService.sumDataSizeByTenantId(tenantId));

        createResource("test", DEFAULT_FILE_NAME);
        assertEquals(4, resourceService.sumDataSizeByTenantId(tenantId));

        for (int i = 2; i < 4; i++) {
            createResource("test" + i, i + DEFAULT_FILE_NAME);
            assertEquals(i * 4, resourceService.sumDataSizeByTenantId(tenantId));
        }
    }

    private TbResourceInfo createResource(String title, String filename) throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setTitle(title);
        resource.setResourceType(ResourceType.JKS);
        resource.setFileName(filename);
        byte[] b = new byte[]{1, 2, 3, 4};
        resource.setData(b);
        return tbResourceService.save(resource);
    }

    @Test
    public void testSaveTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My first resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);

        TbResourceInfo savedResource = tbResourceService.save(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());

        Assert.assertNotNull(foundResource);
        Assert.assertNotNull(foundResource.getId());
        Assert.assertTrue(foundResource.getCreatedTime() > 0);
        assertEquals(resource.getTenantId(), foundResource.getTenantId());
        assertEquals(resource.getTitle(), foundResource.getTitle());
        assertEquals(resource.getResourceKey(), foundResource.getResourceKey());
        assertArrayEquals(TEST_DATA, foundResource.getData());

        String title = "My new resource";

        foundResource.setTitle(title);
        foundResource.setData(null);

        tbResourceService.save(foundResource);
        foundResource = resourceService.findResourceById(tenantId, foundResource.getId());
        assertEquals(title, foundResource.getTitle());
        assertArrayEquals(foundResource.getData(), TEST_DATA);

        tbResourceService.delete(foundResource, true, null);
    }

    @Test
    public void testSaveLwm2mTbResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setFileName("test_model.xml");
        resource.setEncodedData(Base64.getEncoder().encodeToString(LWM2M_TEST_MODEL.getBytes()));

        TbResourceInfo savedResource = tbResourceService.save(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());

        Assert.assertNotNull(foundResource);
        Assert.assertNotNull(foundResource.getId());
        Assert.assertTrue(foundResource.getCreatedTime() > 0);
        assertEquals(resource.getTenantId(), foundResource.getTenantId());
        assertEquals("My first resource id=0 v1.0", foundResource.getTitle());
        assertEquals("0_1.0", foundResource.getResourceKey());
        assertArrayEquals(foundResource.getData(), LWM2M_TEST_MODEL.getBytes());

        tbResourceService.delete(savedResource, true, null);
    }

    @Test
    public void testSaveTbResourceWithEmptyTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);

        assertEquals(TenantId.SYS_TENANT_ID, savedResource.getTenantId());

        tbResourceService.delete(savedResource, true, null);
    }

    @Test
    public void testSaveTbResourceWithSameFileName() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);

        TbResource resource2 = new TbResource();
        resource2.setTenantId(tenantId);
        resource2.setResourceType(ResourceType.JKS);
        resource2.setTitle("My resource");
        resource2.setFileName(DEFAULT_FILE_NAME);
        resource2.setData(TEST_DATA);
        TbResourceInfo savedResource2 = tbResourceService.save(resource2);

        assertThat(savedResource2.getId()).isNotEqualTo(savedResource.getId());
        assertThat(savedResource2.getFileName()).isEqualTo("test.jks");
        assertThat(savedResource2.getResourceKey()).isEqualTo("test_(1).jks");
    }

    @Test
    public void testSaveTbResourceWithEmptyTitle() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.JKS);
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        Assertions.assertThrows(DataValidationException.class, () -> {
            tbResourceService.save(resource);
        });
    }

    @Test
    public void testSaveTbResourceWithInvalidTenant() throws Exception {
        TbResource resource = new TbResource();
        resource.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        Assertions.assertThrows(DataValidationException.class, () -> {
            tbResourceService.save(resource);
        });
    }

    @Test
    public void testSaveLwm2mTbResourceWithXXE() {
        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setFileName("xxe_test_model.xml");
        resource.setData(LWM2M_TEST_MODEL_WITH_XXE.getBytes());

        DataValidationException thrown = assertThrows(DataValidationException.class, () -> {
            tbResourceService.save(resource);
        });
        assertEquals("Failed to parse file xxe_test_model.xml", thrown.getMessage());
    }


    @Test
    public void testFindResourceById() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);

        TbResource foundResource = resourceService.findResourceById(tenantId, savedResource.getId());
        Assert.assertNotNull(foundResource);
        assertEquals(savedResource, new TbResourceInfo(foundResource));
        assertArrayEquals(TEST_DATA, foundResource.getData());
        tbResourceService.delete(foundResource, true, null);
    }

    @Test
    public void testFindResourceByTenantIdAndResourceTypeAndResourceKey() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTenantId(tenantId);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);

        TbResource foundResource = resourceService.findResourceByTenantIdAndKey(tenantId, savedResource.getResourceType(), savedResource.getResourceKey());
        Assert.assertNotNull(foundResource);
        assertEquals(savedResource, new TbResourceInfo(foundResource));
        assertArrayEquals(TEST_DATA, foundResource.getData());
        tbResourceService.delete(foundResource, true, null);
    }

    @Test
    public void testDeleteResource() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My resource");
        resource.setFileName(DEFAULT_FILE_NAME);
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);

        TbResource foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        tbResourceService.delete(savedResource, true, null);
        foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNull(foundResource);
    }

    @Test
    public void testUnForceDeleteResourceAssignWithWidget() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My resource");
        resource.setFileName(JS_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);
        TbResource foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        String link = DataConstants.TB_RESOURCE_PREFIX + resource.getLink();

        WidgetTypeDetails widgetTypeDetails = new WidgetTypeDetails();
        widgetTypeDetails.setTenantId(savedTenant.getId());
        widgetTypeDetails.setDescriptor(JacksonUtil.newObjectNode()
                .put("sizeX", 3)
                .put("sizeY", 3)
                .put("resource", link)
                .put("templateCss", "")
                .put("controllerScript", "self.onInit = function() {\n    self.ctx.$scope.actionWidget.onInit();\n}\n\nself.typeParameters = function() {\n    return {\n        previewWidth: '300px',\n        previewHeight: '320px',\n        embedTitlePanel: true,\n        targetDeviceOptional: true,\n        displayRpcMessageToast: false\n    };\n};\n\nself.onDestroy = function() {\n}")
                .put("settingsSchema", "")
                .put("dataKeySettingsSchema", "{}\n")
                .put("settingsDirective", "tb-scada-symbol-widget-settings")
                .put("hasBasicMode", true)
                .put("basicModeDirective", "tb-scada-symbol-basic-config"));
        widgetTypeDetails.setName("Widget Type");

        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetTypeDetails);
        WidgetTypeDetails foundWidgetType = widgetTypeService.findWidgetTypeDetailsById(savedTenant.getId(), savedWidgetType.getId());
        String resourceLink = foundWidgetType.getDescriptor().get("resource").asText();
        Assertions.assertNotNull(resourceLink);
        Assert.assertEquals(resourceLink, link);

        TbResourceDeleteResult result = tbResourceService.delete(savedResource, false, null);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertFalse(result.getReferences().isEmpty());
        Assert.assertEquals(1, result.getReferences().size());

        EntityInfo widgetTypeInfo = (EntityInfo) result.getReferences().get(EntityType.WIDGET_TYPE.name()).get(0);
        Assert.assertNotNull(widgetTypeInfo);
        Assert.assertEquals(widgetTypeInfo, new EntityInfo(foundWidgetType.getId(), foundWidgetType.getName()));

        TbResourceInfo foundResourceInfo = resourceService.findResourceInfoById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedResource, foundResourceInfo);
    }

    @Test
    public void testForceDeleteResourceAssignWithWidget() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My resource");
        resource.setFileName(JS_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);
        TbResource foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        String link = DataConstants.TB_RESOURCE_PREFIX + resource.getLink();

        WidgetTypeDetails widgetTypeDetails = new WidgetTypeDetails();
        widgetTypeDetails.setTenantId(savedTenant.getId());
        widgetTypeDetails.setDescriptor(JacksonUtil.newObjectNode()
                .put("type", "rpc")
                .put("sizeX", 3)
                .put("sizeY", 3)
                .put("resource", link)
                .put("templateCss", "")
                .put("controllerScript", "self.onInit = function() {\n    self.ctx.$scope.actionWidget.onInit();\n}\n\nself.typeParameters = function() {\n    return {\n        previewWidth: '300px',\n        previewHeight: '320px',\n        embedTitlePanel: true,\n        targetDeviceOptional: true,\n        displayRpcMessageToast: false\n    };\n};\n\nself.onDestroy = function() {\n}")
                .put("settingsSchema", "")
                .put("dataKeySettingsSchema", "{}\n")
                .put("settingsDirective", "tb-scada-symbol-widget-settings")
                .put("hasBasicMode", true)
                .put("basicModeDirective", "tb-scada-symbol-basic-config"));
        widgetTypeDetails.setName("Widget Type");

        WidgetTypeDetails savedWidgetType = widgetTypeService.saveWidgetType(widgetTypeDetails);
        WidgetTypeDetails foundWidgetType = widgetTypeService.findWidgetTypeDetailsById(savedTenant.getId(), savedWidgetType.getId());
        String resourceLink = foundWidgetType.getDescriptor().get("resource").asText();
        Assertions.assertNotNull(resourceLink);
        Assert.assertEquals(resourceLink, link);

        TbResourceDeleteResult result = tbResourceService.delete(savedResource, true, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getReferences());

        foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNull(foundResource);
    }

    @Test
    public void testUnForceDeleteResourceAssignWithDashboard() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My resource");
        resource.setFileName(JS_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);
        TbResource foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        String link = DataConstants.TB_RESOURCE_PREFIX + resource.getLink();

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(savedTenant.getId());
        dashboard.setConfiguration(JacksonUtil.newObjectNode()
                .put("widgets", """
                        {"xxx":
                        {"config":{"actions":{"elementClick":[
                        {"customResources":[{"url":{"entityType":"TB_RESOURCE","id":
                        "tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js"},"isModule":true},
                        {"url":"tb-resource;/api/resource/js_module/tenant/gateway-management-extension.js","isModule":true}]}]}}}}
                        """)
                .put("someResource", link));

        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(savedTenant.getId(), savedDashboard.getId());
        String resourceLink = foundDashboard.getConfiguration().get("someResource").asText();
        Assertions.assertNotNull(resourceLink);
        Assert.assertEquals(resourceLink, link);

        TbResourceDeleteResult result = tbResourceService.delete(savedResource, false, null);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSuccess());
        Assert.assertNotNull(result.getReferences());
        Assert.assertEquals(1, result.getReferences().size());

        EntityInfo dashboardInfo = (EntityInfo) result.getReferences().get(EntityType.DASHBOARD.name()).get(0);
        Assert.assertNotNull(dashboardInfo);
        Assert.assertEquals(new EntityInfo(savedDashboard.getId(), savedDashboard.getName()), dashboardInfo);

        foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        Assert.assertEquals(savedDashboard, foundDashboard);
    }

    @Test
    public void testForceDeleteResourceAssignWithDashboard() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JS_MODULE);
        resource.setTitle("My resource");
        resource.setFileName(JS_FILE_NAME);
        resource.setTenantId(savedTenant.getId());
        resource.setData(TEST_DATA);
        TbResourceInfo savedResource = tbResourceService.save(resource);
        TbResource foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNotNull(foundResource);
        String link = DataConstants.TB_RESOURCE_PREFIX + resource.getLink();

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
                .put("someResource", link));

        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(savedTenant.getId(), savedDashboard.getId());
        String resourceLink = foundDashboard.getConfiguration().get("someResource").asText();
        Assertions.assertNotNull(resourceLink);
        Assert.assertEquals(resourceLink, link);

        TbResourceDeleteResult result = tbResourceService.delete(savedResource, true, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getReferences());

        foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        Assert.assertNull(foundResource);
    }

    @Test
    public void testShouldNotDeleteResourceIfUsedInAiNode() throws Exception {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.GENERAL);
        resource.setTitle("My resource");
        resource.setFileName("test.json");
        resource.setTenantId(savedTenant.getId());
        resource.setData("<test></test>".getBytes());
        TbResourceInfo savedResource = tbResourceService.save(resource);
        RuleChainMetaData ruleChain = createRuleChainReferringResource(savedResource.getId());

        TbResourceDeleteResult result = tbResourceService.delete(savedResource, false, null);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReferences()).isNotEmpty().hasSize(1);
        EntityInfo entityInfo = (EntityInfo) result.getReferences().get(EntityType.RULE_CHAIN.name()).get(0);
        assertThat(entityInfo).isEqualTo(new EntityInfo(ruleChain.getRuleChainId(), "Test"));

        TbResource foundResource = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        assertThat(foundResource).isNotNull();

        // force delete
        TbResourceDeleteResult deleteResult = tbResourceService.delete(savedResource, true, null);
        assertThat(deleteResult).isNotNull();
        assertThat(deleteResult.isSuccess()).isTrue();

        TbResource resourceAfterDeletion = resourceService.findResourceById(savedTenant.getId(), savedResource.getId());
        assertThat(resourceAfterDeletion).isNull();
    }

    private RuleChainMetaData createRuleChainReferringResource(TbResourceId resourceId) {
        AiModel model = constructValidOpenAiModel("Test model");
        AiModel saved = aiModelService.save(model);

        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName("Test");
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChainId ruleChainId = ruleChain.getId();

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChainId);

        RuleNode aiNode = new RuleNode();
        aiNode.setName("Ai request");
        aiNode.setType(org.thingsboard.rule.engine.ai.TbAiNode.class.getName());
        aiNode.setConfigurationVersion(TbAiNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        aiNode.setDebugSettings(DebugSettings.all());
        TbAiNodeConfiguration configuration = new TbAiNodeConfiguration();
        configuration.setResourceIds(Set.of(resourceId.getId()));
        configuration.setModelId(saved.getId());
        configuration.setResponseFormat(new TbResponseFormat.TbJsonResponseFormat());
        configuration.setTimeoutSeconds(1);
        configuration.setUserPrompt("What is temp");
        aiNode.setConfiguration(JacksonUtil.valueToTree(configuration));

        metaData.setNodes(Arrays.asList(aiNode));
        metaData.setFirstNodeIndex(0);
        ruleChainService.saveRuleChainMetaData(tenantId, metaData, Function.identity());
        return ruleChainService.loadRuleChainMetaData(tenantId, ruleChainId);
    }

    private AiModel constructValidOpenAiModel(String name) {
        var modelConfig = OpenAiChatModelConfig.builder()
                .providerConfig(OpenAiProviderConfig.builder()
                        .baseUrl(OpenAiProviderConfig.OPENAI_OFFICIAL_BASE_URL)
                        .apiKey("test-api-key")
                        .build())
                .modelId("gpt-4o")
                .temperature(0.5)
                .topP(0.3)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .maxOutputTokens(1000)
                .timeoutSeconds(60)
                .maxRetries(2)
                .build();

        return AiModel.builder()
                .tenantId(tenantId)
                .name(name)
                .configuration(modelConfig)
                .build();
    }

    @Test
    public void testFindTenantResourcesByTenantId() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 165; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setTitle("Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData(TEST_DATA);
            resources.add(new TbResourceInfo(tbResourceService.save(resource)));
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<TbResourceInfo> pageData;
        do {
            TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                    .tenantId(tenantId)
                    .build();
            pageData = resourceService.findTenantResourcesByTenantId(filter, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        assertEquals(resources, loadedResources);

        resourceService.deleteResourcesByTenantId(tenantId);

        pageLink = new PageLink(31);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .build();
        pageData = resourceService.findTenantResourcesByTenantId(filter, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        deleteTenant(tenantId);
    }

    @Test
    public void testFindAllTenantResourcesByTenantId() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<TbResourceInfo> resources = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(TenantId.SYS_TENANT_ID);
            resource.setTitle("System Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData(TEST_DATA);
            TbResourceInfo tbResourceInfo = new TbResourceInfo(tbResourceService.save(resource));
            if (i >= 50) {
                resources.add(tbResourceInfo);
            }
        }

        for (int i = 0; i < 50; i++) {
            TbResource resource = new TbResource();
            resource.setTenantId(tenantId);
            resource.setTitle("Tenant Resource" + i);
            resource.setResourceType(ResourceType.JKS);
            resource.setFileName(i + DEFAULT_FILE_NAME);
            resource.setData(TEST_DATA);
            resources.add(tbResourceService.save(resource));
        }

        List<TbResourceInfo> loadedResources = new ArrayList<>();
        PageLink pageLink = new PageLink(10);
        PageData<TbResourceInfo> pageData;
        do {
            TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                    .tenantId(tenantId)
                    .build();
            pageData = resourceService.findAllTenantResourcesByTenantId(filter, pageLink);
            loadedResources.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(resources, idComparator);
        Collections.sort(loadedResources, idComparator);

        assertEquals(resources, loadedResources);

        resourceService.deleteResourcesByTenantId(tenantId);

        pageLink = new PageLink(100);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .build();
        pageData = resourceService.findAllTenantResourcesByTenantId(filter, pageLink);
        Assert.assertFalse(pageData.hasNext());
        assertEquals(pageData.getData().size(), 100);

        resourceService.deleteResourcesByTenantId(TenantId.SYS_TENANT_ID);

        pageLink = new PageLink(100);
        filter = TbResourceInfoFilter.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .build();
        pageData = resourceService.findAllTenantResourcesByTenantId(filter, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        deleteTenant(tenantId);
    }

}
