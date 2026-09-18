// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.device.provision;


import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.credentials.ProvisionDeviceCredentialsData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.device.DeviceProvisionServiceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DeviceProvisionServiceTest.TestConfig.class)
public class DeviceProvisionServiceTest {

    @Configuration
    static class TestConfig {
        @Bean
        public DeviceProvisionServiceImpl deviceProvisionService(TbQueueProducerProvider producerProvider,
                                                                 DeviceProfileService deviceProfileService,
                                                                 DeviceService deviceService,
                                                                 DeviceCredentialsService deviceCredentialsService,
                                                                 AttributesService attributesService,
                                                                 AuditLogService auditLogService,
                                                                 PartitionService partitionService) {
            return new DeviceProvisionServiceImpl(producerProvider, deviceProfileService, deviceService,
                    deviceCredentialsService, attributesService, auditLogService, partitionService);
        }
    }

    @MockitoBean
    protected TbQueueProducerProvider producerProvider;
    @MockitoBean
    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMsgProducer;
    @MockitoBean
    protected TbClusterService clusterService;
    @MockitoBean
    protected DeviceProfileService deviceProfileService;
    @MockitoBean
    protected DeviceService deviceService;
    @MockitoBean
    protected DeviceCredentialsService deviceCredentialsService;
    @MockitoBean
    protected AttributesService attributesService;
    @MockitoBean
    protected AuditLogService auditLogService;
    @MockitoBean
    protected PartitionService partitionService;
    @MockitoSpyBean
    DeviceProvisionServiceImpl service;

    private String[] chain;

    @Before
    public void setUp() {
        String filePath = "src/test/resources/provision/x509ChainProvisionTest.pem";
        try {
            String certificateChain = Files.readString(Paths.get(filePath));
            certificateChain = certTrimNewLinesForChainInDeviceProfile(certificateChain);
            chain = fetchLeafCertificateFromChain(certificateChain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void provisionDeviceViaX509Certificate() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], true);

        var device = createDevice(tenant.getId(), deviceProfile.getId());
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(any(), any())).thenReturn(deviceCredentials);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        ProvisionResponse response = service.provisionDeviceViaX509Chain(deviceProfile, createProvisionRequest(chain[0]));

        verify(deviceService, times(1)).findDeviceByTenantIdAndName(any(), any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByDeviceId(any(), any());
        verify(deviceCredentialsService, times(1)).updateDeviceCredentials(any(), any());

        Assertions.assertThat(response.getResponseStatus()).isEqualTo(ProvisionResponseStatus.SUCCESS);
        Assertions.assertThat(response.getDeviceCredentials()).isEqualTo(deviceCredentials);
    }

    @Test
    public void provisionDeviceWithIncorrectConfiguration() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], false);

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(deviceProfile, createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class);

        verify(deviceService, times(1)).findDeviceByTenantIdAndName(any(), any());
    }

    @Test
    public void matchDeviceNameFromX509CNCertificateByRegex() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], true);
        X509CertificateChainProvisionConfiguration configuration = (X509CertificateChainProvisionConfiguration) deviceProfile.getProfileData().getProvisionConfiguration();
        String CN = getCNFromX509Certificate(chain[0]);
        String deviceName = service.extractDeviceNameFromCNByRegEx(deviceProfile, CN, configuration.getCertificateRegExPattern());

        Assertions.assertThat(deviceName).isNotBlank();
        Assertions.assertThat(deviceName).isEqualTo("deviceCertificate");
    }

    @Test
    public void matchDeviceNameFromCNByRegex() {
        var CN = "DeviceA.company.com";
        var regex = "(.*)\\.company.com";
        var result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "DeviceA@company.com";
        regex = "(.*)@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "prefixDeviceAsuffix@company.com";
        regex = "prefix(.*)suffix@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "prefixDeviceAsufix@company.com";
        regex = "prefix(.*)sufix@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");

        CN = "region.DeviceA.220423@company.com";
        regex = "\\D+\\.(.*)\\.\\d+@company.com";
        result = service.extractDeviceNameFromCNByRegEx(null, CN, regex);
        Assertions.assertThat(result).isNotBlank();
        Assertions.assertThat(result).isEqualTo("DeviceA");
    }

    private static final String MATCHING_CN_REGEX = "(deviceCert.*)";
    private static final String NON_MATCHING_CN_REGEX = "(otherDevice.*)";

    @Test
    public void singleCandidateDelegatesWithoutCommonNameSelection() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], true);
        deviceProfile.getProfileData().setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(List.of(deviceProfile), createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class)
                .hasMessage("Device profile provision strategy is not X509_CERTIFICATE_CHAIN!");
    }

    @Test
    public void singleCandidateIsProvisionedEvenWhenRegexOnlyMatchesLoosely() {
        var tenant = createTenant();
        var deviceProfile = createDeviceProfile(tenant.getId(), chain[1], true, MATCHING_CN_REGEX);

        var device = createDevice(tenant.getId(), deviceProfile.getId());
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(any(), any())).thenReturn(deviceCredentials);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        ProvisionResponse response = service.provisionDeviceViaX509Chain(List.of(deviceProfile), createProvisionRequest(chain[0]));

        Assertions.assertThat(response.getResponseStatus()).isEqualTo(ProvisionResponseStatus.SUCCESS);
    }

    @Test
    public void selectProfileBySharedCertificateAndMatchingCommonName() {
        var tenant = createTenant();
        var matchingProfile = createDeviceProfile(tenant.getId(), chain[1], false, MATCHING_CN_REGEX);
        var otherProfile = createDeviceProfile(tenant.getId(), chain[1], false, NON_MATCHING_CN_REGEX);

        var device = createDevice(tenant.getId(), matchingProfile.getId());
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);

        var deviceCredentials = createDeviceCredentials(chain[0], device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(any(), any())).thenReturn(deviceCredentials);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        ProvisionResponse response = service.provisionDeviceViaX509Chain(
                List.of(otherProfile, matchingProfile), createProvisionRequest(chain[0]));

        Assertions.assertThat(response.getResponseStatus()).isEqualTo(ProvisionResponseStatus.SUCCESS);
        verify(deviceService, times(1)).findDeviceByTenantIdAndName(tenant.getId(), "deviceCertificate@X509ProvisionStrategy");
    }

    @Test
    public void failWhenSeveralProfilesMatchCommonName() {
        var tenant = createTenant();
        var first = createDeviceProfile(tenant.getId(), chain[1], true, MATCHING_CN_REGEX);
        var second = createDeviceProfile(tenant.getId(), chain[1], true, "(device.*)");

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(List.of(first, second), createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class);

        verify(deviceService, never()).saveDevice(any());
        verify(deviceService, never()).findDeviceByTenantIdAndName(any(), any());
    }

    @Test
    public void failWhenNoProfileMatchesCommonName() {
        var tenant = createTenant();
        var first = createDeviceProfile(tenant.getId(), chain[1], true, NON_MATCHING_CN_REGEX);
        var second = createDeviceProfile(tenant.getId(), chain[1], true, "(yetAnother.*)");

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(List.of(first, second), createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class);

        verify(deviceService, never()).findDeviceByTenantIdAndName(any(), any());
    }

    @Test
    public void failWhenSharedProfileRegexIsBlankOrInvalid() {
        var tenant = createTenant();
        for (String regex : new String[]{null, "", "   ", "["}) {
            var unusable = createDeviceProfile(tenant.getId(), chain[1], true, regex);
            var other = createDeviceProfile(tenant.getId(), chain[1], true, NON_MATCHING_CN_REGEX);

            Assertions.assertThatThrownBy(() ->
                            service.provisionDeviceViaX509Chain(List.of(unusable, other), createProvisionRequest(chain[0])))
                    .as("CN regular expression [%s] must not be selectable", regex)
                    .isInstanceOf(ProvisionFailedException.class);
        }
    }

    @Test
    public void failWhenCommonNameCannotBeParsed() {
        var tenant = createTenant();
        var first = createDeviceProfile(tenant.getId(), chain[1], true, MATCHING_CN_REGEX);
        var second = createDeviceProfile(tenant.getId(), chain[1], true, NON_MATCHING_CN_REGEX);

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(List.of(first, second), createProvisionRequest("not a certificate")))
                .isInstanceOf(ProvisionFailedException.class);
    }

    @Test
    public void failWhenNoCandidateProfileIsGiven() {
        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain(List.of(), createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class)
                .hasMessage("Device profile is not specified!");

        Assertions.assertThatThrownBy(() ->
                        service.provisionDeviceViaX509Chain((List<DeviceProfile>) null, createProvisionRequest(chain[0])))
                .isInstanceOf(ProvisionFailedException.class)
                .hasMessage("Device profile is not specified!");
    }

    private DeviceProfile createDeviceProfile(TenantId tenantId, String certificateValue, boolean isAllowToCreateNewDevices) {
        return createDeviceProfile(tenantId, certificateValue, isAllowToCreateNewDevices, "([^@]+)");
    }

    private DeviceProfile createDeviceProfile(TenantId tenantId, String certificateValue, boolean isAllowToCreateNewDevices,
                                              String certificateRegExPattern) {
        X509CertificateChainProvisionConfiguration provision = new X509CertificateChainProvisionConfiguration();
        provision.setProvisionDeviceSecret(certificateValue);
        provision.setCertificateRegExPattern(certificateRegExPattern);
        provision.setAllowCreateNewDevicesByX509Certificate(isAllowToCreateNewDevices);

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setProvisionConfiguration(provision);

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(new DeviceProfileId(UUID.randomUUID()));
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(EncryptionUtil.getSha3Hash(certificateValue));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        deviceProfile.setTenantId(tenantId);
        return deviceProfile;
    }

    private Device createDevice(TenantId tenantId, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setDeviceProfileId(deviceProfileId);
        device.setCustomerId(new CustomerId(UUID.randomUUID()));
        return device;
    }

    private Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TenantId.fromUUID(UUID.randomUUID()));
        return tenant;
    }

    private DeviceCredentials createDeviceCredentials(String certificateValue, DeviceId deviceId) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsValue(certificateValue);
        deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(certificateValue));
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        return deviceCredentials;
    }

    private ProvisionRequest createProvisionRequest(String certificateValue) {
        return new ProvisionRequest(null, DeviceCredentialsType.X509_CERTIFICATE,
                new ProvisionDeviceCredentialsData(null, null, null, null, certificateValue),
                null, null);
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

    private String getCNFromX509Certificate(String x509Value) {
        try {
            return SslUtil.parseCommonName(SslUtil.readCertFile(x509Value));
        } catch (Exception e) {
            return null;
        }
    }
}
