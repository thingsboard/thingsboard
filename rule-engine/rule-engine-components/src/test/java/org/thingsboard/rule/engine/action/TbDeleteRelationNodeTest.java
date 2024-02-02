/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class TbDeleteRelationNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final List<EntityType> supportedEntityTypes = Stream.of(EntityType.TENANT, EntityType.DEVICE,
                    EntityType.ASSET, EntityType.CUSTOMER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE, EntityType.USER)
            .collect(Collectors.toList());

    private static final List<EntityType> unsupportedEntityTypes = Arrays.stream(EntityType.values())
            .filter(type -> !supportedEntityTypes.contains(type)).collect(Collectors.toList());

    @Mock
    private TbContext ctxMock;

    private TbDeleteRelationNode node;
    private TbDeleteRelationNodeConfiguration config;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = spy(new TbDeleteRelationNode());
        config = new TbDeleteRelationNodeConfiguration().defaultConfiguration();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbDeleteRelationNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getDirection()).isEqualTo(EntitySearchDirection.FROM.name());
        assertThat(defaultConfig.getRelationType()).isEqualTo(EntityRelation.CONTAINS_TYPE);
        assertThat(defaultConfig.getEntityNamePattern()).isEqualTo("");
        assertThat(defaultConfig.getEntityTypePattern()).isEqualTo(null);
        assertThat(defaultConfig.getEntityType()).isEqualTo(null);
        assertThat(defaultConfig.isDeleteForSingleEntity()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void givenEntityType_whenInit_thenVerifyExceptionThrownIfTypeIsUnsupported(EntityType entityType) {
        // GIVEN
        config.setEntityType(entityType.name());
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN-THEN
        if (unsupportedEntityTypes.contains(entityType)) {
            assertThatThrownBy(() -> node.init(ctxMock, nodeConfiguration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage(TbAbstractRelationActionNode.unsupportedEntityTypeErrorMessage(entityType.name()));
        } else {
            assertThatCode(() -> node.init(ctxMock, nodeConfiguration)).doesNotThrowAnyException();
        }
        verifyNoInteractions(ctxMock);
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // version 0 config
                Arguments.of(0,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\",\"entityCacheExpiration\":300}",
                        true,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}"),
                // config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}",
                        false,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}")
        );
    }

}
