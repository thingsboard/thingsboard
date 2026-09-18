// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.thingsboard.server.service.lwm2m.LwM2MServiceImpl;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.Lwm2mServerIdentifier.LWM2M_SERVER_MAX;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.Lwm2mServerIdentifier.NOT_USED_IDENTIFYING_LWM2M_SERVER_MAX;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.Lwm2mServerIdentifier.PRIMARY_LWM2M_SERVER;

@ExtendWith(MockitoExtension.class)
public class LwM2MServiceImplInvalidConfigTest {

    @Mock
    private LwM2MTransportServerConfig serverConfig;

    @Mock
    private LwM2MTransportBootstrapConfig bootstrapConfig;

    private LwM2MServiceImpl lwM2MService;

    @BeforeEach
    void setUp() {
        lwM2MService = new LwM2MServiceImpl(serverConfig, Optional.of(bootstrapConfig));
    }

    @Test
    void testGetServerSecurityInfo_BsServerWithNonEmptyId_ClearsToNullAndLogsWarn() {
        given(bootstrapConfig.getId()).willReturn(111);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(true);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isTrue();
        assertThat(result.getShortServerId()).isNull();
    }

    @Test
    void testGetServerSecurityInfo_DmServerWithValidId_Success() {
        Integer validShortServerId = 123;
        given(serverConfig.getId()).willReturn(validShortServerId);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(false);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isFalse();
        assertThat(result.getShortServerId()).isEqualTo(validShortServerId);
    }

    @Test
    void testGetServerSecurityInfo_DmServerWithMinValidId_ReturnedUnchanged() {
        Integer validShortServerId = PRIMARY_LWM2M_SERVER.getId();
        given(serverConfig.getId()).willReturn(validShortServerId);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(false);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isFalse();
        assertThat(result.getShortServerId()).isEqualTo(validShortServerId);
    }

    @Test
    void testGetServerSecurityInfo_DmServerWithMaxValidId_ReturnedUnchanged() {
        Integer validShortServerId = LWM2M_SERVER_MAX.getId();
        given(serverConfig.getId()).willReturn(validShortServerId);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(false);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isFalse();
        assertThat(result.getShortServerId()).isEqualTo(validShortServerId);
    }

    @Test
    void testGetServerSecurityInfo_Server_Less_PRIMARY_LWM2M_SERVER_DefaultsToOneAndLogsWarn() {
        Integer inValidShortServerId = PRIMARY_LWM2M_SERVER.getId() - 1;
        given(serverConfig.getId()).willReturn(inValidShortServerId);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(false);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isFalse();
        assertThat(result.getShortServerId()).isEqualTo(PRIMARY_LWM2M_SERVER.getId());
    }

    @Test
    void testGetServerSecurityInfo_Server_NOT_USED_IDENTIFYING_LWM2M_SERVER_MAX_DefaultsToOneAndLogsWarn() {
        Integer inValidShortServerId = NOT_USED_IDENTIFYING_LWM2M_SERVER_MAX.getId();
        given(serverConfig.getId()).willReturn(inValidShortServerId);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(false);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isFalse();
        assertThat(result.getShortServerId()).isEqualTo(PRIMARY_LWM2M_SERVER.getId());
    }

    @Test
    void testGetServerSecurityInfo_ServerWithNullId_DefaultsToOneAndLogsWarn() {
        Integer inValidShortServerId = null;
        given(serverConfig.getId()).willReturn(inValidShortServerId);
        LwM2MServerSecurityConfigDefault result = lwM2MService.getServerSecurityInfo(false);
        assertThat(result).isNotNull();
        assertThat(result.isBootstrapServerIs()).isFalse();
        assertThat(result.getShortServerId()).isEqualTo(PRIMARY_LWM2M_SERVER.getId());
    }
}
