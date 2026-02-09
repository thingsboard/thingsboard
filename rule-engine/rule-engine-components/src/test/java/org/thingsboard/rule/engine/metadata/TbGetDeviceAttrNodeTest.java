/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TbGetDeviceAttrNodeTest extends AbstractRuleNodeUpgradeTest {

    private final TenantId TENANT_ID = new TenantId(UUID.fromString("5aea576c-66c4-4732-86b8-dc6bfcde7443"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("40b6b393-6ddf-47f9-973a-18550ca70384"));
    private final ListeningExecutor executor = new TestDbCallbackExecutor();


    private TbGetDeviceAttrNode node;
    private TbGetDeviceAttrNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private DeviceService deviceServiceMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbGetDeviceAttrNode());
        config = new TbGetDeviceAttrNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getClientAttributeNames()).isEmpty();
        assertThat(config.getSharedAttributeNames()).isEmpty();
        assertThat(config.getServerAttributeNames()).isEmpty();
        assertThat(config.getLatestTsKeyNames()).isEmpty();
        assertThat(config.isTellFailureIfAbsent()).isTrue();
        assertThat(config.isGetLatestValueWithTs()).isFalse();
        assertThat(config.getFetchTo()).isEqualTo(TbMsgSource.METADATA);

        var deviceRelationsQuery = new DeviceRelationsQuery();
        deviceRelationsQuery.setDirection(EntitySearchDirection.FROM);
        deviceRelationsQuery.setMaxLevel(1);
        deviceRelationsQuery.setRelationType(EntityRelation.CONTAINS_TYPE);
        deviceRelationsQuery.setDeviceTypes(Collections.singletonList("default"));

        assertThat(config.getDeviceRelationsQuery()).isEqualTo(deviceRelationsQuery);
    }

    @Test
    public void givenFetchToIsNull_whenInit_thenThrowsException() {
        config.setFetchTo(null);
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
    }

    @Test
    public void givenDeviceDoesNotExist_whenOnMsg_thenTellFailure() throws TbNodeException {
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        given(ctxMock.getDeviceService()).willReturn(deviceServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(deviceServiceMock.findDevicesByQuery(any(TenantId.class), any(DeviceSearchQuery.class))).willReturn(Futures.immediateFuture(Collections.emptyList()));
        given(ctxMock.getDbCallbackExecutor()).willReturn(executor);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> actualException = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualException.capture());
        assertThat(actualException.getValue())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Failed to find related device to message originator using relation query specified in the configuration!");
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 1 with upgrade from version 0
                Arguments.of(0,
                        """
                                {
                                "fetchToData":false,
                                "clientAttributeNames":[],
                                "sharedAttributeNames":[],
                                "serverAttributeNames":[],
                                "latestTsKeyNames":[],
                                "tellFailureIfAbsent":true,
                                "getLatestValueWithTs":false,
                                "deviceRelationsQuery":{"direction":"FROM","maxLevel":1,"relationType":"Contains","deviceTypes":["default"],"fetchLastLevelOnly":false}
                                }
                        """,
                        true,
                        """
                                {
                                "deviceRelationsQuery": {"direction": "FROM","maxLevel": 1, "relationType": "Contains","deviceTypes": ["default"],"fetchLastLevelOnly": false},
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                ),
                // config for version 1 with upgrade from version 0 (old config with no fetchToData property)
                Arguments.of(0,
                        """
                                {
                                "clientAttributeNames":[],
                                "sharedAttributeNames":[],
                                "serverAttributeNames":[],
                                "latestTsKeyNames":[],
                                "tellFailureIfAbsent":true,
                                "getLatestValueWithTs":false,
                                "deviceRelationsQuery":{"direction":"FROM","maxLevel":1,"relationType":"Contains","deviceTypes":["default"],"fetchLastLevelOnly":false}
                                }
                        """,
                        true,
                        """
                                {
                                "deviceRelationsQuery": {"direction": "FROM","maxLevel": 1, "relationType": "Contains","deviceTypes": ["default"],"fetchLastLevelOnly": false},
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                ),
                // config for version 1 with upgrade from version 0 (old config with null fetchToData property)
                Arguments.of(0,
                        """
                                {
                                "fetchToData":null,
                                "clientAttributeNames":[],
                                "sharedAttributeNames":[],
                                "serverAttributeNames":[],
                                "latestTsKeyNames":[],
                                "tellFailureIfAbsent":true,
                                "getLatestValueWithTs":false,
                                "deviceRelationsQuery":{"direction":"FROM","maxLevel":1,"relationType":"Contains","deviceTypes":["default"],"fetchLastLevelOnly":false}
                                }
                        """,
                        true,
                        """
                                {
                                "deviceRelationsQuery": {"direction": "FROM","maxLevel": 1, "relationType": "Contains","deviceTypes": ["default"],"fetchLastLevelOnly": false},
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                ),
                // config for version 1 with upgrade from version 1
                Arguments.of(1,
                        """
                                {
                                "deviceRelationsQuery": {"direction": "FROM","maxLevel": 1, "relationType": "Contains","deviceTypes": ["default"],"fetchLastLevelOnly": false},
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """,
                        false,
                        """
                                {
                                "deviceRelationsQuery": {"direction": "FROM","maxLevel": 1, "relationType": "Contains","deviceTypes": ["default"],"fetchLastLevelOnly": false},
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                )
        );

    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
