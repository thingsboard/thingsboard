// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.transport;


import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTransportApiServiceTest.TestConfig.class)
public class DefaultTransportApiServiceTest {

    @Configuration
    static class TestConfig {
        @Bean
        public DefaultTransportApiService defaultTransportApiService(TbDeviceProfileCache deviceProfileCache,
                                                                     TbTenantProfileCache tenantProfileCache,
                                                                     TbApiUsageStateService apiUsageStateService,
                                                                     DeviceService deviceService,
                                                                     DeviceProfileService deviceProfileService,
                                                                     RelationService relationService,
                                                                     DeviceCredentialsService deviceCredentialsService,
                                                                     TbClusterService tbClusterService,
                                                                     DeviceProvisionService deviceProvisionService,
                                                                     ResourceService resourceService,
                                                                     OtaPackageService otaPackageService,
                                                                     OtaPackageDataCache otaPackageDataCache,
                                                                     QueueService queueService,
                                                                     EntityGroupService entityGroupService,
                                                                     CustomerService customerService) {
            return new DefaultTransportApiService(deviceProfileCache, tenantProfileCache, apiUsageStateService,
                    deviceService, deviceProfileService, relationService, deviceCredentialsService, tbClusterService,
                    deviceProvisionService, resourceService, otaPackageService, otaPackageDataCache, queueService,
                    entityGroupService, customerService);
        }
    }

    @MockitoBean
    protected TbDeviceProfileCache deviceProfileCache;
    @MockitoBean
    protected TbTenantProfileCache tenantProfileCache;
    @MockitoBean
    protected TbApiUsageStateService apiUsageStateService;
    @MockitoBean
    protected DeviceService deviceService;
    @MockitoBean
    protected DeviceProfileService deviceProfileService;
    @MockitoBean
    protected RelationService relationService;
    @MockitoBean
    protected DeviceCredentialsService deviceCredentialsService;
    @MockitoBean
    protected DbCallbackExecutorService dbCallbackExecutorService;
    @MockitoBean
    protected TbClusterService tbClusterService;
    @MockitoBean
    protected DeviceProvisionService deviceProvisionService;
    @MockitoBean
    protected ResourceService resourceService;
    @MockitoBean
    protected OtaPackageService otaPackageService;
    @MockitoBean
    protected OtaPackageDataCache otaPackageDataCache;
    @MockitoBean
    protected QueueService queueService;
    @MockitoBean
    protected EntityGroupService groupService;
    @MockitoBean
    protected CustomerService customerService;
    @MockitoSpyBean
    DefaultTransportApiService service;

    private String certificateChain;
    private String[] chain;

    @Before
    public void setUp() {

        String filePath = "src/test/resources/provision/x509ChainProvisionTest.pem";
        try {
            certificateChain = Files.readString(Paths.get(filePath));
            certificateChain = certTrimNewLinesForChainInDeviceProfile(certificateChain);
            chain = fetchLeafCertificateFromChain(certificateChain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void validateExistingDeviceByX509CertificateStrategy() {
        var device = createDevice();

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(any())).thenReturn(deviceCredentials);

        TransportProtos.TransportApiResponseMsg response = mock(TransportProtos.TransportApiResponseMsg.class);
        willReturn(response).given(service).getDeviceInfo(deviceCredentials, false);

        service.validateOrCreateDeviceX509Certificate(certificateChain, false);
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByCredentialsId(any());
    }

    @Test
    public void provisionDeviceX509Certificate() {
        var deviceProfile = createDeviceProfile(chain[1]);
        when(deviceProfileService.findDeviceProfileByProvisionDeviceKey(any())).thenReturn(deviceProfile);

        var device = createDevice();
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(any())).thenReturn(null);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        var provisionResponse = createProvisionResponse(deviceCredentials);
        when(deviceProvisionService.provisionDeviceViaX509Chain(any(), any())).thenReturn(provisionResponse);

        TransportProtos.TransportApiResponseMsg response = mock(TransportProtos.TransportApiResponseMsg.class);
        willReturn(response).given(service).getDeviceInfo(deviceCredentials, false);

        service.validateOrCreateDeviceX509Certificate(certificateChain, false);
        verify(deviceProfileService, times(1)).findDeviceProfileByProvisionDeviceKey(any());
        verify(service, times(1)).getDeviceInfo(any(), eq(false));
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByCredentialsId(any());
        verify(deviceProvisionService, times(1)).provisionDeviceViaX509Chain(any(), any());
    }

    @Test
    public void givenProfileCachingSupported_whenValidateToken_thenNoProfileInResponseAndVersionSet() throws Exception {
        DeviceProfile profile = prepareDeviceWithProfile("token", DeviceCredentialsType.ACCESS_TOKEN, 7L);

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = validateToken("token", true);

        assertThat(response.hasDeviceProfile()).isFalse();
        assertThat(response.getDeviceProfileVersion()).isEqualTo(profile.getVersion());
        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.VALIDATION_OK);
    }

    @Test
    public void givenProfileCachingNotSupported_whenValidateToken_thenFullProfilePresent() throws Exception {
        prepareDeviceWithProfile("token", DeviceCredentialsType.ACCESS_TOKEN, 7L);

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = validateToken("token", false);

        assertThat(response.hasDeviceProfile()).isTrue();
        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.VALIDATION_OK);
    }

    @Test
    public void givenUnknownToken_whenValidateToken_thenCredentialsNotFoundResult() throws Exception {
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId("unknown")).thenReturn(null);

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = validateToken("unknown", true);

        assertThat(response.hasDeviceInfo()).isFalse();
        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.CREDENTIALS_NOT_FOUND);
    }

    @Test
    public void givenDeviceLookupFails_whenValidateToken_thenInternalErrorResult() throws Exception {
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setDeviceId(new DeviceId(UUID.randomUUID()));
        credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        credentials.setCredentialsId("token");
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId("token")).thenReturn(credentials);
        when(deviceService.findDeviceById(any(), any())).thenReturn(null);

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = validateToken("token", true);

        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.VALIDATION_INTERNAL_ERROR);
    }

    @Test
    public void givenProfileCachingSupported_whenValidateX509_thenNoProfileInResponse() throws Exception {
        prepareDeviceWithProfile("hash", DeviceCredentialsType.X509_CERTIFICATE, 7L);

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = handleValidation(
                TransportProtos.TransportApiRequestMsg.newBuilder()
                        .setValidateX509CertRequestMsg(TransportProtos.ValidateDeviceX509CertRequestMsg.newBuilder()
                                .setHash("hash")
                                .setSupportsDeviceProfileCaching(true))
                        .build());

        assertThat(response.hasDeviceProfile()).isFalse();
        assertThat(response.getDeviceProfileVersion()).isEqualTo(7L);
        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.VALIDATION_OK);
    }

    @Test
    public void givenDeviceProfileNotFound_whenValidateToken_thenInternalErrorResultWithDeviceInfo() throws Exception {
        prepareDeviceWithProfile("token", DeviceCredentialsType.ACCESS_TOKEN, 7L);
        when(deviceProfileCache.get(any(), any(DeviceProfileId.class))).thenReturn(null);

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = validateToken("token", true);

        assertThat(response.hasDeviceInfo()).isTrue();
        assertThat(response.hasDeviceProfile()).isFalse();
        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.VALIDATION_INTERNAL_ERROR);
    }

    @Test
    public void givenX509ProvisionFails_whenValidateOrCreateX509_thenInternalErrorResult() throws Exception {
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(any())).thenReturn(null);
        when(deviceProfileService.findDeviceProfileByProvisionDeviceKey(any())).thenReturn(createDeviceProfile(chain[1]));
        when(deviceProvisionService.provisionDeviceViaX509Chain(any(), any())).thenThrow(new ProvisionFailedException("limit"));

        TransportProtos.ValidateDeviceCredentialsResponseMsg response = handleValidation(
                TransportProtos.TransportApiRequestMsg.newBuilder()
                        .setValidateOrCreateX509CertRequestMsg(TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg.newBuilder()
                                .setCertificateChain(certificateChain))
                        .build());

        assertThat(response.hasDeviceInfo()).isFalse();
        assertThat(response.getResult()).isEqualTo(TransportProtos.CredentialsValidationResult.VALIDATION_INTERNAL_ERROR);
    }

    private DeviceProfile prepareDeviceWithProfile(String token, DeviceCredentialsType credentialsType, long profileVersion) {
        DeviceProfile profile = new DeviceProfile();
        profile.setId(new DeviceProfileId(UUID.randomUUID()));
        profile.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        profile.setName("profile");
        profile.setType(DeviceProfileType.DEFAULT);
        profile.setTransportType(DeviceTransportType.DEFAULT);
        profile.setProfileData(new DeviceProfileData());
        profile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        profile.setVersion(profileVersion);

        Device device = createDevice();
        device.setTenantId(profile.getTenantId());
        device.setName("device");
        device.setType("default");
        device.setAdditionalInfo(JacksonUtil.newObjectNode());
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        device.setDeviceProfileId(profile.getId());

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setDeviceId(device.getId());
        credentials.setCredentialsType(credentialsType);
        credentials.setCredentialsId(token);
        credentials.setCredentialsValue(token);

        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(token)).thenReturn(credentials);
        when(deviceService.findDeviceById(any(), any())).thenReturn(device);
        when(deviceProfileCache.get(any(), any(DeviceProfileId.class))).thenReturn(profile);
        return profile;
    }

    private TransportProtos.ValidateDeviceCredentialsResponseMsg validateToken(String token, boolean supportsProfileCaching) throws Exception {
        return handleValidation(TransportProtos.TransportApiRequestMsg.newBuilder()
                .setValidateTokenRequestMsg(TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder()
                        .setToken(token)
                        .setSupportsDeviceProfileCaching(supportsProfileCaching))
                .build());
    }

    private TransportProtos.ValidateDeviceCredentialsResponseMsg handleValidation(TransportProtos.TransportApiRequestMsg request) throws Exception {
        return service.handle(new TbProtoQueueMsg<>(UUID.randomUUID(), request))
                .get(10, TimeUnit.SECONDS).getValue().getValidateCredResponseMsg();
    }

    private DeviceProfile createDeviceProfile(String certificateValue) {
        X509CertificateChainProvisionConfiguration provision = new X509CertificateChainProvisionConfiguration();
        provision.setProvisionDeviceSecret(certificateValue);
        provision.setCertificateRegExPattern("([^@]+)");
        provision.setAllowCreateNewDevicesByX509Certificate(true);

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setProvisionConfiguration(provision);

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(EncryptionUtil.getSha3Hash(certificateValue));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        return deviceProfile;
    }

    private DeviceCredentials createDeviceCredentials(String certificateValue, DeviceId deviceId) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsValue(certificateValue);
        deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(certificateValue));
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        return deviceCredentials;
    }

    private Device createDevice() {
        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        return device;
    }

    private ProvisionResponse createProvisionResponse(DeviceCredentials deviceCredentials) {
        return new ProvisionResponse(deviceCredentials, ProvisionResponseStatus.SUCCESS);
    }

    public static String certTrimNewLinesForChainInDeviceProfile(String input) {
        return input.replaceAll("\n", "")
                .replaceAll("\r", "")
                .replaceAll("-----BEGIN CERTIFICATE-----", "-----BEGIN CERTIFICATE-----\n")
                .replaceAll("-----END CERTIFICATE-----", "\n-----END CERTIFICATE-----\n")
                .trim();
    }

    private String[] fetchLeafCertificateFromChain(String value) {
        List<String> chain = new ArrayList<>();
        String regex = "-----BEGIN CERTIFICATE-----\\s*.*?\\s*-----END CERTIFICATE-----";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            chain.add(matcher.group(0));
        }
        return chain.toArray(new String[0]);
    }
}
