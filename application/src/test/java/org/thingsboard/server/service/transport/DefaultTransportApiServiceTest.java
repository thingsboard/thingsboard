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
package org.thingsboard.server.service.transport;


import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.resource.TbResourceService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTransportApiService.class)
public class DefaultTransportApiServiceTest {

    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;
    @MockBean
    protected TbTenantProfileCache tenantProfileCache;
    @MockBean
    protected TbApiUsageStateService apiUsageStateService;
    @MockBean
    protected DeviceService deviceService;
    @MockBean
    protected DeviceProfileService deviceProfileService;
    @MockBean
    protected RelationService relationService;
    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;
    @MockBean
    protected DbCallbackExecutorService dbCallbackExecutorService;
    @MockBean
    protected TbClusterService tbClusterService;
    @MockBean
    protected DataDecodingEncodingService dataDecodingEncodingService;
    @MockBean
    protected DeviceProvisionService deviceProvisionService;
    @MockBean
    protected TbResourceService resourceService;
    @MockBean
    protected OtaPackageService otaPackageService;
    @MockBean
    protected OtaPackageDataCache otaPackageDataCache;
    @MockBean
    protected QueueService queueService;
    @SpyBean
    DefaultTransportApiService service;

    private final String deviceCertificate = "-----BEGIN CERTIFICATE-----Device certificate value-----END CERTIFICATE-----";
    private final String deviceProfileCertificate = "-----BEGIN CERTIFICATE-----Device profile certificate value-----END CERTIFICATE-----";

    @Test
    public void validateExistingDeviceX509Certificate() {
        var device = createDevice();
        when(deviceService.findDeviceByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(device));

        var deviceCredentials = createDeviceCredentials(deviceCertificate, device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(any())).thenReturn(deviceCredentials);

        service.validateOrCreateDeviceX509Certificate(deviceCertificate, DeviceCredentialsType.X509_CERTIFICATE);
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByCredentialsId(any());
    }

    @Test
    public void updateExistingDeviceX509Certificate() {
        var deviceProfile = createDeviceProfile(deviceProfileCertificate);
        when(deviceProfileService.findDeviceProfileByCertificateHash(any())).thenReturn(deviceProfile);

        var device = createDevice();
        when(deviceService.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);
        when(deviceService.findDeviceByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(device));

        var deviceCredentials = createDeviceCredentials(deviceCertificate, device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(any(), any())).thenReturn(deviceCredentials);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        service.validateOrCreateDeviceX509Certificate(deviceProfileCertificate, DeviceCredentialsType.X509_CERTIFICATE);
        verify(deviceProfileService, times(1)).findDeviceProfileByCertificateHash(any());
        verify(deviceService, times(1)).findDeviceByTenantIdAndName(any(), any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByDeviceId(any(), any());
        verify(deviceCredentialsService, times(1)).updateDeviceCredentials(any(), any());
    }

    @Test
    public void createDeviceByX509Provision() {
        var deviceProfile = createDeviceProfile(deviceProfileCertificate);
        when(deviceProfileService.findDeviceProfileByCertificateHash(any())).thenReturn(deviceProfile);

        var device = createDevice();
        when(deviceService.saveDevice(any())).thenReturn(device);
        when(deviceService.findDeviceByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(device));

        var deviceCredentials = createDeviceCredentials(deviceCertificate, device.getId());
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(any(), any())).thenReturn(deviceCredentials);
        when(deviceCredentialsService.updateDeviceCredentials(any(), any())).thenReturn(deviceCredentials);

        service.validateOrCreateDeviceX509Certificate(deviceProfileCertificate, DeviceCredentialsType.X509_CERTIFICATE);
        verify(deviceProfileService, times(1)).findDeviceProfileByCertificateHash(any());
        verify(deviceService, times(1)).findDeviceByTenantIdAndName(any(), any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByDeviceId(any(), any());
        verify(deviceCredentialsService, times(1)).updateDeviceCredentials(any(), any());
    }

    private DeviceCredentials createDeviceCredentials(String certificateValue, DeviceId deviceId) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsValue(certificateValue);
        deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(certificateValue));
        deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
        return deviceCredentials;
    }

    private DeviceProfile createDeviceProfile(String certificateValue) {
        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        X509CertificateChainProvisionConfiguration provision = new X509CertificateChainProvisionConfiguration();
        provision.setCertificateValue(certificateValue);
        provision.setCertificateRegExPattern("^$");
        provision.setAllowCreateNewDevicesByX509Certificate(true);
        deviceProfileData.setProvisionConfiguration(provision);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setCertificateHash(EncryptionUtil.getSha3Hash(certificateValue));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN);
        return deviceProfile;
    }

    private Device createDevice() {
        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        return device;
    }
}
