/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;

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
    private TenantProfileService tenantProfileService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private RuleChainService ruleChainService;

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
    public void createDefaultTenantProfiles() throws Exception {
        tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);

        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());

        TenantProfile isolatedTbCoreProfile = new TenantProfile();
        isolatedTbCoreProfile.setDefault(false);
        isolatedTbCoreProfile.setName("Isolated TB Core");
        isolatedTbCoreProfile.setDescription("Isolated TB Core tenant profile");
        isolatedTbCoreProfile.setIsolatedTbCore(true);
        isolatedTbCoreProfile.setIsolatedTbRuleEngine(false);
        isolatedTbCoreProfile.setProfileData(tenantProfileData);
        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbCoreProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }

        TenantProfile isolatedTbRuleEngineProfile = new TenantProfile();
        isolatedTbRuleEngineProfile.setDefault(false);
        isolatedTbRuleEngineProfile.setName("Isolated TB Rule Engine");
        isolatedTbRuleEngineProfile.setDescription("Isolated TB Rule Engine tenant profile");
        isolatedTbRuleEngineProfile.setIsolatedTbCore(false);
        isolatedTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbRuleEngineProfile.setProfileData(tenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }

        TenantProfile isolatedTbCoreAndTbRuleEngineProfile = new TenantProfile();
        isolatedTbCoreAndTbRuleEngineProfile.setDefault(false);
        isolatedTbCoreAndTbRuleEngineProfile.setName("Isolated TB Core and TB Rule Engine");
        isolatedTbCoreAndTbRuleEngineProfile.setDescription("Isolated TB Core and TB Rule Engine tenant profile");
        isolatedTbCoreAndTbRuleEngineProfile.setIsolatedTbCore(true);
        isolatedTbCoreAndTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbCoreAndTbRuleEngineProfile.setProfileData(tenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbCoreAndTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setKey("general");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        node.put("prohibitDifferentUrl", true);
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
        node.put("enableProxy", false);
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);
    }

    @Override
    public void createOAuth2Templates() throws Exception {
        installScripts.createOAuth2Templates();
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

        DeviceProfile defaultDeviceProfile = this.deviceProfileService.findOrCreateDeviceProfile(demoTenant.getId(), DEFAULT_DEVICE_TYPE);

        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A1", "A1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A2", "A2_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A3", "A3_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerB.getId(), defaultDeviceProfile.getId(), "Test Device B1", "B1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerC.getId(), defaultDeviceProfile.getId(), "Test Device C1", "C1_TEST_TOKEN", null);

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "DHT11 Demo Device", "DHT11_DEMO_TOKEN", "Demo device that is used in sample " +
                "applications that upload data from DHT11 temperature and humidity sensor");

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "Raspberry Pi Demo Device", "RASPBERRY_PI_DEMO_TOKEN", "Demo device that is used in " +
                "Raspberry Pi GPIO control sample application");

        DeviceProfile thermostatDeviceProfile = new DeviceProfile();
        thermostatDeviceProfile.setTenantId(demoTenant.getId());
        thermostatDeviceProfile.setDefault(false);
        thermostatDeviceProfile.setName("thermostat");
        thermostatDeviceProfile.setType(DeviceProfileType.DEFAULT);
        thermostatDeviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        thermostatDeviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        thermostatDeviceProfile.setDescription("Thermostat device profile");
        thermostatDeviceProfile.setDefaultRuleChainId(ruleChainService.findTenantRuleChains(
                demoTenant.getId(), new PageLink(1, 0, "Thermostat")).getData().get(0).getId());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        thermostatDeviceProfile.setProfileData(deviceProfileData);

        DeviceProfileAlarm highTemperature = new DeviceProfileAlarm();
        highTemperature.setId("highTemperatureAlarmID");
        highTemperature.setAlarmType("High Temperature");
        AlarmRule temperatureRule = new AlarmRule();
        AlarmCondition temperatureCondition = new AlarmCondition();
        temperatureCondition.setSpec(new SimpleAlarmConditionSpec());

        KeyFilter temperatureAlarmFlagAttributeFilter = new KeyFilter();
        temperatureAlarmFlagAttributeFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperatureAlarmFlag"));
        temperatureAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate temperatureAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        temperatureAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        temperatureAlarmFlagAttributePredicate.setValue(new FilterPredicateValue<>(Boolean.TRUE));
        temperatureAlarmFlagAttributeFilter.setPredicate(temperatureAlarmFlagAttributePredicate);

        KeyFilter temperatureTimeseriesFilter = new KeyFilter();
        temperatureTimeseriesFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        temperatureTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperatureTimeseriesFilterPredicate = new NumericFilterPredicate();
        temperatureTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        FilterPredicateValue<Double> temperatureTimeseriesPredicateValue =
                new FilterPredicateValue<>(25.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "temperatureAlarmThreshold"));
        temperatureTimeseriesFilterPredicate.setValue(temperatureTimeseriesPredicateValue);
        temperatureTimeseriesFilter.setPredicate(temperatureTimeseriesFilterPredicate);
        temperatureCondition.setCondition(Arrays.asList(temperatureAlarmFlagAttributeFilter, temperatureTimeseriesFilter));
        temperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        temperatureRule.setCondition(temperatureCondition);
        highTemperature.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.MAJOR, temperatureRule)));

        AlarmRule clearTemperatureRule = new AlarmRule();
        AlarmCondition clearTemperatureCondition = new AlarmCondition();
        clearTemperatureCondition.setSpec(new SimpleAlarmConditionSpec());

        KeyFilter clearTemperatureTimeseriesFilter = new KeyFilter();
        clearTemperatureTimeseriesFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        clearTemperatureTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate clearTemperatureTimeseriesFilterPredicate = new NumericFilterPredicate();
        clearTemperatureTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS_OR_EQUAL);
        FilterPredicateValue<Double> clearTemperatureTimeseriesPredicateValue =
                new FilterPredicateValue<>(25.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "temperatureAlarmThreshold"));

        clearTemperatureTimeseriesFilterPredicate.setValue(clearTemperatureTimeseriesPredicateValue);
        clearTemperatureTimeseriesFilter.setPredicate(clearTemperatureTimeseriesFilterPredicate);
        clearTemperatureCondition.setCondition(Collections.singletonList(clearTemperatureTimeseriesFilter));
        clearTemperatureRule.setCondition(clearTemperatureCondition);
        clearTemperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        highTemperature.setClearRule(clearTemperatureRule);

        DeviceProfileAlarm lowHumidity = new DeviceProfileAlarm();
        lowHumidity.setId("lowHumidityAlarmID");
        lowHumidity.setAlarmType("Low Humidity");
        AlarmRule humidityRule = new AlarmRule();
        AlarmCondition humidityCondition = new AlarmCondition();
        humidityCondition.setSpec(new SimpleAlarmConditionSpec());

        KeyFilter humidityAlarmFlagAttributeFilter = new KeyFilter();
        humidityAlarmFlagAttributeFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "humidityAlarmFlag"));
        humidityAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate humidityAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        humidityAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        humidityAlarmFlagAttributePredicate.setValue(new FilterPredicateValue<>(Boolean.TRUE));
        humidityAlarmFlagAttributeFilter.setPredicate(humidityAlarmFlagAttributePredicate);

        KeyFilter humidityTimeseriesFilter = new KeyFilter();
        humidityTimeseriesFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "humidity"));
        humidityTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate humidityTimeseriesFilterPredicate = new NumericFilterPredicate();
        humidityTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        FilterPredicateValue<Double> humidityTimeseriesPredicateValue =
                new FilterPredicateValue<>(60.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "humidityAlarmThreshold"));
        humidityTimeseriesFilterPredicate.setValue(humidityTimeseriesPredicateValue);
        humidityTimeseriesFilter.setPredicate(humidityTimeseriesFilterPredicate);
        humidityCondition.setCondition(Arrays.asList(humidityAlarmFlagAttributeFilter, humidityTimeseriesFilter));

        humidityRule.setCondition(humidityCondition);
        humidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidity.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.MINOR, humidityRule)));

        AlarmRule clearHumidityRule = new AlarmRule();
        AlarmCondition clearHumidityCondition = new AlarmCondition();
        clearHumidityCondition.setSpec(new SimpleAlarmConditionSpec());

        KeyFilter clearHumidityTimeseriesFilter = new KeyFilter();
        clearHumidityTimeseriesFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "humidity"));
        clearHumidityTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate clearHumidityTimeseriesFilterPredicate = new NumericFilterPredicate();
        clearHumidityTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL);
        FilterPredicateValue<Double> clearHumidityTimeseriesPredicateValue =
                new FilterPredicateValue<>(60.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "humidityAlarmThreshold"));

        clearHumidityTimeseriesFilterPredicate.setValue(clearHumidityTimeseriesPredicateValue);
        clearHumidityTimeseriesFilter.setPredicate(clearHumidityTimeseriesFilterPredicate);
        clearHumidityCondition.setCondition(Collections.singletonList(clearHumidityTimeseriesFilter));
        clearHumidityRule.setCondition(clearHumidityCondition);
        clearHumidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidity.setClearRule(clearHumidityRule);

        deviceProfileData.setAlarms(Arrays.asList(highTemperature, lowHumidity));

        DeviceProfile savedThermostatDeviceProfile = deviceProfileService.saveDeviceProfile(thermostatDeviceProfile);

        DeviceId t1Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T1", "T1_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();
        DeviceId t2Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T2", "T2_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();

        attributesService.save(demoTenant.getId(), t1Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.3948)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -122.1503)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 20)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 50))));

        attributesService.save(demoTenant.getId(), t2Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.493801)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -121.948769)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 25)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 30))));

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
                                DeviceProfileId deviceProfileId,
                                String name,
                                String accessToken,
                                String description) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setDeviceProfileId(deviceProfileId);
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
        mainQueue.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
        queueService.createOrUpdateQueue(mainQueue);

        Queue highPriorityQueue = new Queue();
        highPriorityQueue.setTenantId(TenantId.SYS_TENANT_ID);
        highPriorityQueue.setName("HighPriority");
        highPriorityQueue.setTopic("tb_rule_engine.hp");
        highPriorityQueue.setPollInterval(25);
        highPriorityQueue.setPartitions(10);
        highPriorityQueue.setPackProcessingTimeout(2000);
        SubmitStrategy highPriorityQueueSubmitStrategy = new SubmitStrategy();
        highPriorityQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        highPriorityQueueSubmitStrategy.setBatchSize(100);
        highPriorityQueue.setSubmitStrategy(highPriorityQueueSubmitStrategy);
        ProcessingStrategy highPriorityQueueProcessingStrategy = new ProcessingStrategy();
        highPriorityQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
        highPriorityQueueProcessingStrategy.setRetries(0);
        highPriorityQueueProcessingStrategy.setFailurePercentage(0);
        highPriorityQueueProcessingStrategy.setPauseBetweenRetries(5);
        highPriorityQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
        highPriorityQueue.setProcessingStrategy(highPriorityQueueProcessingStrategy);
        queueService.createOrUpdateQueue(highPriorityQueue);

        Queue sequentialByOriginatorQueue = new Queue();
        sequentialByOriginatorQueue.setTenantId(TenantId.SYS_TENANT_ID);
        sequentialByOriginatorQueue.setName("SequentialByOriginator");
        sequentialByOriginatorQueue.setTopic("tb_rule_engine.sq");
        sequentialByOriginatorQueue.setPollInterval(25);
        sequentialByOriginatorQueue.setPartitions(10);
        sequentialByOriginatorQueue.setPackProcessingTimeout(2000);
        SubmitStrategy sequentialByOriginatorQueueSubmitStrategy = new SubmitStrategy();
        sequentialByOriginatorQueueSubmitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        sequentialByOriginatorQueueSubmitStrategy.setBatchSize(100);
        sequentialByOriginatorQueue.setSubmitStrategy(sequentialByOriginatorQueueSubmitStrategy);
        ProcessingStrategy sequentialByOriginatorQueueProcessingStrategy = new ProcessingStrategy();
        sequentialByOriginatorQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
        sequentialByOriginatorQueueProcessingStrategy.setRetries(3);
        sequentialByOriginatorQueueProcessingStrategy.setFailurePercentage(0);
        sequentialByOriginatorQueueProcessingStrategy.setPauseBetweenRetries(5);
        sequentialByOriginatorQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
        sequentialByOriginatorQueue.setProcessingStrategy(sequentialByOriginatorQueueProcessingStrategy);
        queueService.createOrUpdateQueue(sequentialByOriginatorQueue);
    }
}
