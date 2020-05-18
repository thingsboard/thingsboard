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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.Arrays;

@Service
@Profile("install")
@Slf4j
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String CUSTOMER_CRED = "customer";
    public static final String DEFAULT_DEVICE_TYPE = "default";

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private QueueService queueService;

    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void createSysAdmin() {
        createUser(Authority.SYS_ADMIN, null, null, "sysadmin@thingsboard.org", "sysadmin");
    }

    @Override
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setKey("general");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, generalSettings);

        AdminSettings mailSettings = new AdminSettings();
        mailSettings.setKey("mail");
        node = objectMapper.createObjectNode();
        node.put("mailFrom", "ThingsBoard <sysadmin@localhost.localdomain>");
        node.put("smtpProtocol", "smtp");
        node.put("smtpHost", "localhost");
        node.put("smtpPort", "25");
        node.put("timeout", "10000");
        node.put("enableTls", false);
        node.put("username", "");
        node.put("password", "");
        node.put("tlsVersion", "TLSv1.2");//NOSONAR, key used to identify password field (not password value itself)
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);
    }

    @Override
    public void loadDemoData() throws Exception {
        Tenant demoTenant = new Tenant();
        demoTenant.setRegion("Global");
        demoTenant.setTitle("Tenant");
        demoTenant = tenantService.saveTenant(demoTenant);
        installScripts.loadDemoRuleChains(demoTenant.getId());
        createUser(Authority.TENANT_ADMIN, demoTenant.getId(), null, "tenant@thingsboard.org", "tenant");

        Customer customerA = new Customer();
        customerA.setTenantId(demoTenant.getId());
        customerA.setTitle("Customer A");
        customerA = customerService.saveCustomer(customerA);
        Customer customerB = new Customer();
        customerB.setTenantId(demoTenant.getId());
        customerB.setTitle("Customer B");
        customerB = customerService.saveCustomer(customerB);
        Customer customerC = new Customer();
        customerC.setTenantId(demoTenant.getId());
        customerC.setTitle("Customer C");
        customerC = customerService.saveCustomer(customerC);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customer@thingsboard.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customerA@thingsboard.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerB.getId(), "customerB@thingsboard.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerC.getId(), "customerC@thingsboard.org", CUSTOMER_CRED);

        createDevice(demoTenant.getId(), customerA.getId(), DEFAULT_DEVICE_TYPE, "Test Device A1", "A1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), DEFAULT_DEVICE_TYPE, "Test Device A2", "A2_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), DEFAULT_DEVICE_TYPE, "Test Device A3", "A3_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerB.getId(), DEFAULT_DEVICE_TYPE, "Test Device B1", "B1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerC.getId(), DEFAULT_DEVICE_TYPE, "Test Device C1", "C1_TEST_TOKEN", null);

        createDevice(demoTenant.getId(), null, DEFAULT_DEVICE_TYPE, "DHT11 Demo Device", "DHT11_DEMO_TOKEN", "Demo device that is used in sample " +
                "applications that upload data from DHT11 temperature and humidity sensor");

        createDevice(demoTenant.getId(), null, DEFAULT_DEVICE_TYPE, "Raspberry Pi Demo Device", "RASPBERRY_PI_DEMO_TOKEN", "Demo device that is used in " +
                "Raspberry Pi GPIO control sample application");

        Asset thermostatAlarms = new Asset();
        thermostatAlarms.setTenantId(demoTenant.getId());
        thermostatAlarms.setName("Thermostat Alarms");
        thermostatAlarms.setType("AlarmPropagationAsset");
        thermostatAlarms = assetService.saveAsset(thermostatAlarms);

        DeviceId t1Id = createDevice(demoTenant.getId(), null, "thermostat", "Thermostat T1", "T1_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();
        DeviceId t2Id = createDevice(demoTenant.getId(), null, "thermostat", "Thermostat T2", "T2_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();

        relationService.saveRelation(thermostatAlarms.getTenantId(), new EntityRelation(thermostatAlarms.getId(), t1Id, "ToAlarmPropagationAsset"));
        relationService.saveRelation(thermostatAlarms.getTenantId(), new EntityRelation(thermostatAlarms.getId(), t2Id, "ToAlarmPropagationAsset"));

        attributesService.save(demoTenant.getId(), t1Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.3948)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -122.1503)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("alarmTemperature", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("alarmHumidity", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("thresholdTemperature", (long) 20)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("thresholdHumidity", (long) 50))));

        attributesService.save(demoTenant.getId(), t2Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.493801)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -121.948769)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("alarmTemperature", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("alarmHumidity", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("thresholdTemperature", (long) 25)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("thresholdHumidity", (long) 30))));

        installScripts.loadDashboards(demoTenant.getId(), null);
    }

    @Override
    public void deleteSystemWidgetBundle(String bundleAlias) throws Exception {
        WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, bundleAlias);
        if (widgetsBundle != null) {
            widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, widgetsBundle.getId());
        }
    }

    @Override
    public void loadSystemWidgets() throws Exception {
        installScripts.loadSystemWidgets();
    }

    @Override
    public void updateSystemWidgets() throws Exception {
        this.deleteSystemWidgetBundle("charts");
        this.deleteSystemWidgetBundle("cards");
        this.deleteSystemWidgetBundle("maps");
        this.deleteSystemWidgetBundle("analogue_gauges");
        this.deleteSystemWidgetBundle("digital_gauges");
        this.deleteSystemWidgetBundle("gpio_widgets");
        this.deleteSystemWidgetBundle("alarm_widgets");
        this.deleteSystemWidgetBundle("control_widgets");
        this.deleteSystemWidgetBundle("maps_v2");
        this.deleteSystemWidgetBundle("gateway_widgets");
        this.deleteSystemWidgetBundle("input_widgets");
        this.deleteSystemWidgetBundle("date");
        this.deleteSystemWidgetBundle("entity_admin_widgets");
        installScripts.loadSystemWidgets();
    }

    private User createUser(Authority authority,
                            TenantId tenantId,
                            CustomerId customerId,
                            String email,
                            String password) {
        User user = new User();
        user.setAuthority(authority);
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user = userService.saveUser(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
        userCredentials.setPassword(passwordEncoder.encode(password));
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
        return user;
    }

    private Device createDevice(TenantId tenantId,
                                CustomerId customerId,
                                String type,
                                String name,
                                String accessToken,
                                String description) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setType(type);
        device.setName(name);
        if (description != null) {
            ObjectNode additionalInfo = objectMapper.createObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(TenantId.SYS_TENANT_ID, deviceCredentials);
        return device;
    }

    @Override
    public void createQueues() {
        Queue mainQueue = new Queue();
        mainQueue.setTenantId(TenantId.SYS_TENANT_ID);
        mainQueue.setName("Main");
        mainQueue.setTopic("tb_rule_engine.main");
        mainQueue.setPollInterval(25);
        mainQueue.setPartitions(10);
        mainQueue.setPackProcessingTimeout(60000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
        queueService.createOrUpdateQueue(mainQueue);

        Queue highPriorityQueue = new Queue();
        highPriorityQueue.setTenantId(TenantId.SYS_TENANT_ID);
        highPriorityQueue.setName("HighPriority");
        highPriorityQueue.setTopic("tb_rule_engine.hp");
        highPriorityQueue.setPollInterval(25);
        highPriorityQueue.setPartitions(10);
        highPriorityQueue.setPackProcessingTimeout(60000);
        SubmitStrategy highPriorityQueueSubmitStrategy = new SubmitStrategy();
        highPriorityQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        highPriorityQueueSubmitStrategy.setBatchSize(100);
        highPriorityQueue.setSubmitStrategy(highPriorityQueueSubmitStrategy);
        ProcessingStrategy highPriorityQueueProcessingStrategy = new ProcessingStrategy();
        highPriorityQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
        highPriorityQueueProcessingStrategy.setRetries(0);
        highPriorityQueueProcessingStrategy.setFailurePercentage(0);
        highPriorityQueueProcessingStrategy.setPauseBetweenRetries(5);
        highPriorityQueue.setProcessingStrategy(highPriorityQueueProcessingStrategy);
        queueService.createOrUpdateQueue(highPriorityQueue);

        Queue sequentialByOriginatorQueue = new Queue();
        sequentialByOriginatorQueue.setTenantId(TenantId.SYS_TENANT_ID);
        sequentialByOriginatorQueue.setName("SequentialByOriginator");
        sequentialByOriginatorQueue.setTopic("tb_rule_engine.sq");
        sequentialByOriginatorQueue.setPollInterval(25);
        sequentialByOriginatorQueue.setPartitions(10);
        sequentialByOriginatorQueue.setPackProcessingTimeout(60000);
        SubmitStrategy sequentialByOriginatorQueueSubmitStrategy = new SubmitStrategy();
        sequentialByOriginatorQueueSubmitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        sequentialByOriginatorQueueSubmitStrategy.setBatchSize(100);
        sequentialByOriginatorQueue.setSubmitStrategy(sequentialByOriginatorQueueSubmitStrategy);
        ProcessingStrategy sequentialByOriginatorQueueProcessingStrategy = new ProcessingStrategy();
        sequentialByOriginatorQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
        sequentialByOriginatorQueueProcessingStrategy.setRetries(3);
        sequentialByOriginatorQueueProcessingStrategy.setFailurePercentage(0);
        sequentialByOriginatorQueueProcessingStrategy.setPauseBetweenRetries(5);
        sequentialByOriginatorQueue.setProcessingStrategy(sequentialByOriginatorQueueProcessingStrategy);
        queueService.createOrUpdateQueue(sequentialByOriginatorQueue);
    }
}
