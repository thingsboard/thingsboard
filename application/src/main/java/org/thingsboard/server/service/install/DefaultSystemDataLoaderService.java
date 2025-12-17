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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.AttributeScope;
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
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.ComplexOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.SimpleAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.BooleanFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceConnectivityConfiguration;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.common.util.DebugModeUtil.DEBUG_MODE_DEFAULT_DURATION_MINUTES;
import static org.thingsboard.server.common.data.DataConstants.DEFAULT_DEVICE_TYPE;
import static org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService.isSigningKeyDefault;
import static org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService.validateKeyLength;

@Service
@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    public static final String CUSTOMER_CRED = "customer";
    public static final String ACTIVITY_STATE = "active";

    private final InstallScripts installScripts;
    private final UserService userService;
    private final AdminSettingsService adminSettingsService;
    private final TenantService tenantService;
    private final TenantProfileService tenantProfileService;
    private final CustomerService customerService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final AttributesService attributesService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final RuleChainService ruleChainService;
    private final TimeseriesService tsService;
    private final DeviceConnectivityConfiguration connectivityConfiguration;
    private final QueueService queueService;
    private final JwtSettingsService jwtSettingsService;
    private final MobileAppDao mobileAppDao;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationTargetService notificationTargetService;
    private final CalculatedFieldService calculatedFieldService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    private boolean persistActivityToTelemetry;

    @Value("${security.jwt.tokenExpirationTime:9000}")
    private Integer tokenExpirationTime;
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private Integer refreshTokenExpTime;
    @Value("${security.jwt.tokenIssuer:thingsboard.io}")
    private String tokenIssuer;
    @Value("${security.jwt.tokenSigningKey:thingsboardDefaultSigningKey}")
    private String tokenSigningKey;

    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("sys-loader-ts-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void createSysAdmin() {
        createUser(Authority.SYS_ADMIN, null, null, "sysadmin@thingsboard.org", "sysadmin");
    }

    @Override
    public void createDefaultTenantProfiles() throws Exception {
        tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);

        TenantProfileData isolatedRuleEngineTenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration configuration = new DefaultTenantProfileConfiguration();
        configuration.setMaxDebugModeDurationMinutes(DEBUG_MODE_DEFAULT_DURATION_MINUTES);
        isolatedRuleEngineTenantProfileData.setConfiguration(configuration);

        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);

        isolatedRuleEngineTenantProfileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));

        TenantProfile isolatedTbRuleEngineProfile = new TenantProfile();
        isolatedTbRuleEngineProfile.setDefault(false);
        isolatedTbRuleEngineProfile.setName("Isolated TB Rule Engine");
        isolatedTbRuleEngineProfile.setDescription("Isolated TB Rule Engine tenant profile");
        isolatedTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbRuleEngineProfile.setProfileData(isolatedRuleEngineTenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setTenantId(TenantId.SYS_TENANT_ID);
        generalSettings.setKey("general");
        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        node.put("prohibitDifferentUrl", false);
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, generalSettings);

        AdminSettings mailSettings = new AdminSettings();
        mailSettings.setTenantId(TenantId.SYS_TENANT_ID);
        mailSettings.setKey("mail");
        node = JacksonUtil.newObjectNode();
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
        node.put("showChangePassword", false);
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);

        AdminSettings connectivitySettings = new AdminSettings();
        connectivitySettings.setTenantId(TenantId.SYS_TENANT_ID);
        connectivitySettings.setKey("connectivity");
        connectivitySettings.setJsonValue(JacksonUtil.valueToTree(connectivityConfiguration.getConnectivity()));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, connectivitySettings);
    }

    @Override
    public void createRandomJwtSettings() throws Exception {
        if (jwtSettingsService.getJwtSettings() == null) {
            log.info("Creating JWT admin settings...");
            var jwtSettings = new JwtSettings(this.tokenExpirationTime, this.refreshTokenExpTime, this.tokenIssuer, this.tokenSigningKey);
            if (isSigningKeyDefault(jwtSettings) || !validateKeyLength(jwtSettings.getTokenSigningKey())) {
                jwtSettings.setTokenSigningKey(generateRandomKey());
            }
            jwtSettingsService.saveJwtSettings(jwtSettings);
        } else {
            log.info("Skip creating JWT admin settings because they already exist.");
        }
    }

    @Override
    public void updateSecuritySettings() {
        JwtSettings jwtSettings = jwtSettingsService.getJwtSettings();
        boolean invalidSignKey = false;
        String warningMessage = null;

        if (isSigningKeyDefault(jwtSettings)) {
            warningMessage = "The platform is using the default JWT Signing Key, which is a security risk.";
            invalidSignKey = true;
        } else if (!validateKeyLength(jwtSettings.getTokenSigningKey())) {
            warningMessage = "The JWT Signing Key is shorter than 512 bits, which is a security risk.";
            invalidSignKey = true;
        }

        if (invalidSignKey) {
            log.warn("WARNING: {}. A new JWT Signing Key has been added automatically. " +
                     "You can change the JWT Signing Key using the Web UI: " +
                     "Navigate to \"System settings -> Security settings\" while logged in as a System Administrator.", warningMessage);

            jwtSettings.setTokenSigningKey(generateRandomKey());
            jwtSettingsService.saveJwtSettings(jwtSettings);
        }

        List<MobileApp> mobiles = mobileAppDao.findByTenantId(TenantId.SYS_TENANT_ID, null, new PageLink(Integer.MAX_VALUE, 0)).getData();
        if (CollectionUtils.isNotEmpty(mobiles)) {
            mobiles.stream()
                    .filter(mobileApp -> !validateKeyLength(mobileApp.getAppSecret()))
                    .forEach(mobileApp -> {
                        log.warn("WARNING: The App secret is shorter than 512 bits, which is a security risk. " +
                                 "A new Application Secret has been added automatically for Mobile Application [{}]. " +
                                 "You can change the Application Secret using the Web UI: " +
                                 "Navigate to \"Security settings -> OAuth2 -> Mobile applications\" while logged in as a System Administrator.", mobileApp.getPkgName());
                        mobileApp.setAppSecret(generateRandomKey());
                        mobileAppDao.save(TenantId.SYS_TENANT_ID, mobileApp);
                    });
        }
    }

    private String generateRandomKey() {
        return Base64.getEncoder().encodeToString(
                RandomStringUtils.randomAlphanumeric(64).getBytes(StandardCharsets.UTF_8));
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

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "DHT11 Demo Device", "DHT11_DEMO_TOKEN",
                "Demo device that is used in sample applications that upload data from DHT11 temperature and humidity sensor");

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "Raspberry Pi Demo Device", "RASPBERRY_PI_DEMO_TOKEN",
                "Demo device that is used in Raspberry Pi GPIO control sample application");

        DeviceProfile thermostatDeviceProfile = new DeviceProfile();
        thermostatDeviceProfile.setTenantId(demoTenant.getId());
        thermostatDeviceProfile.setDefault(false);
        thermostatDeviceProfile.setName("thermostat");
        thermostatDeviceProfile.setType(DeviceProfileType.DEFAULT);
        thermostatDeviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        thermostatDeviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        thermostatDeviceProfile.setDescription("Thermostat device profile");
        thermostatDeviceProfile.setDefaultRuleChainId(ruleChainService.findTenantRuleChainsByType(
                demoTenant.getId(), RuleChainType.CORE, new PageLink(1, 0, "Thermostat")).getData().get(0).getId());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        thermostatDeviceProfile.setProfileData(deviceProfileData);

        DeviceProfile savedThermostatDeviceProfile = deviceProfileService.saveDeviceProfile(thermostatDeviceProfile);
        createAlarmRules(demoTenant.getId(), savedThermostatDeviceProfile.getId());

        DeviceId t1Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T1", "T1_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();
        DeviceId t2Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T2", "T2_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();

        attributesService.save(demoTenant.getId(), t1Id, AttributeScope.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.3948)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -122.1503)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 20)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 50))));

        attributesService.save(demoTenant.getId(), t2Id, AttributeScope.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.493801)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -121.948769)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 25)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 30))));

        installScripts.loadDashboards(demoTenant.getId(), null);
        installScripts.createDefaultTenantDashboards(demoTenant.getId(), null);
    }

    private void createAlarmRules(TenantId tenantId, DeviceProfileId deviceProfileId) {
        CalculatedField highTemperature = new CalculatedField();
        highTemperature.setName("High Temperature");
        highTemperature.setType(CalculatedFieldType.ALARM);
        highTemperature.setTenantId(tenantId);
        highTemperature.setEntityId(deviceProfileId);
        highTemperature.setDebugSettings(DebugSettings.all());
        AlarmCalculatedFieldConfiguration highTemperatureConfig = new AlarmCalculatedFieldConfiguration();
        highTemperature.setConfiguration(highTemperatureConfig);

        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        Argument temperatureThresholdArgument = new Argument();
        temperatureThresholdArgument.setRefEntityKey(new ReferencedEntityKey("temperatureAlarmThreshold", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        temperatureThresholdArgument.setDefaultValue("25");
        Argument temperatureAlarmFlagArgument = new Argument();
        temperatureAlarmFlagArgument.setRefEntityKey(new ReferencedEntityKey("temperatureAlarmFlag", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        highTemperatureConfig.setArguments(Map.of(
                "temperature", temperatureArgument,
                "temperatureAlarmThreshold", temperatureThresholdArgument,
                "temperatureAlarmFlag", temperatureAlarmFlagArgument
        ));

        AlarmRule temperatureRule = new AlarmRule();
        SimpleAlarmCondition temperatureCondition = new SimpleAlarmCondition();

        AlarmConditionFilter temperatureAlarmFlagFilter = new AlarmConditionFilter();
        temperatureAlarmFlagFilter.setArgument("temperatureAlarmFlag");
        temperatureAlarmFlagFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate temperatureAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        temperatureAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        temperatureAlarmFlagAttributePredicate.setValue(new AlarmConditionValue<>(Boolean.TRUE, null));
        temperatureAlarmFlagFilter.setPredicates(List.of(temperatureAlarmFlagAttributePredicate));

        AlarmConditionFilter temperatureFilter = new AlarmConditionFilter();
        temperatureFilter.setArgument("temperature");
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperatureFilterPredicate = new NumericFilterPredicate();
        temperatureFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        temperatureFilterPredicate.setValue(new AlarmConditionValue<>(null, "temperatureAlarmThreshold"));
        temperatureFilter.setPredicates(List.of(temperatureFilterPredicate));
        temperatureCondition.setExpression(new SimpleAlarmConditionExpression(List.of(temperatureAlarmFlagFilter, temperatureFilter), ComplexOperation.AND));
        temperatureRule.setCondition(temperatureCondition);
        temperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        highTemperatureConfig.setCreateRules(Map.of(
                AlarmSeverity.MAJOR, temperatureRule
        ));

        AlarmRule clearTemperatureRule = new AlarmRule();
        SimpleAlarmCondition clearTemperatureCondition = new SimpleAlarmCondition();

        AlarmConditionFilter clearTemperatureFilter = new AlarmConditionFilter();
        clearTemperatureFilter.setArgument("temperature");
        clearTemperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate clearTemperatureFilterPredicate = new NumericFilterPredicate();
        clearTemperatureFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS_OR_EQUAL);
        clearTemperatureFilterPredicate.setValue(new AlarmConditionValue<>(null, "temperatureAlarmThreshold"));
        clearTemperatureFilter.setPredicates(List.of(clearTemperatureFilterPredicate));
        clearTemperatureCondition.setExpression(new SimpleAlarmConditionExpression(List.of(clearTemperatureFilter), ComplexOperation.AND));
        clearTemperatureRule.setCondition(clearTemperatureCondition);
        clearTemperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        highTemperatureConfig.setClearRule(clearTemperatureRule);

        calculatedFieldService.save(highTemperature);

        CalculatedField lowHumidity = new CalculatedField();
        lowHumidity.setName("Low Humidity");
        lowHumidity.setType(CalculatedFieldType.ALARM);
        lowHumidity.setTenantId(tenantId);
        lowHumidity.setEntityId(deviceProfileId);
        lowHumidity.setDebugSettings(DebugSettings.all());
        AlarmCalculatedFieldConfiguration lowHumidityConfig = new AlarmCalculatedFieldConfiguration();
        lowHumidity.setConfiguration(lowHumidityConfig);

        Argument humidityArgument = new Argument();
        humidityArgument.setRefEntityKey(new ReferencedEntityKey("humidity", ArgumentType.TS_LATEST, null));
        Argument humidityThresholdArgument = new Argument();
        humidityThresholdArgument.setRefEntityKey(new ReferencedEntityKey("humidityAlarmThreshold", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        humidityThresholdArgument.setDefaultValue("60");
        Argument humidityAlarmFlagArgument = new Argument();
        humidityAlarmFlagArgument.setRefEntityKey(new ReferencedEntityKey("humidityAlarmFlag", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        lowHumidityConfig.setArguments(Map.of(
                "humidity", humidityArgument,
                "humidityAlarmThreshold", humidityThresholdArgument,
                "humidityAlarmFlag", humidityAlarmFlagArgument
        ));

        AlarmRule humidityRule = new AlarmRule();
        SimpleAlarmCondition humidityCondition = new SimpleAlarmCondition();

        AlarmConditionFilter humidityAlarmFlagAttributeFilter = new AlarmConditionFilter();
        humidityAlarmFlagAttributeFilter.setArgument("humidityAlarmFlag");
        humidityAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate humidityAlarmFlagPredicate = new BooleanFilterPredicate();
        humidityAlarmFlagPredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        humidityAlarmFlagPredicate.setValue(new AlarmConditionValue<>(Boolean.TRUE, null));
        humidityAlarmFlagAttributeFilter.setPredicates(List.of(humidityAlarmFlagPredicate));

        AlarmConditionFilter humidityFilter = new AlarmConditionFilter();
        humidityFilter.setArgument("humidity");
        humidityFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate humidityFilterPredicate = new NumericFilterPredicate();
        humidityFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        humidityFilterPredicate.setValue(new AlarmConditionValue<>(null, "humidityAlarmThreshold"));
        humidityFilter.setPredicates(List.of(humidityFilterPredicate));
        humidityCondition.setExpression(new SimpleAlarmConditionExpression(List.of(humidityAlarmFlagAttributeFilter, humidityFilter), ComplexOperation.AND));
        humidityRule.setCondition(humidityCondition);
        humidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidityConfig.setCreateRules(Map.of(
                AlarmSeverity.MINOR, humidityRule
        ));

        AlarmRule clearHumidityRule = new AlarmRule();
        SimpleAlarmCondition clearHumidityCondition = new SimpleAlarmCondition();

        AlarmConditionFilter clearHumidityFilter = new AlarmConditionFilter();
        clearHumidityFilter.setArgument("humidity");
        clearHumidityFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate clearHumidityFilterPredicate = new NumericFilterPredicate();
        clearHumidityFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL);
        clearHumidityFilterPredicate.setValue(new AlarmConditionValue<>(null, "humidityAlarmThreshold"));
        clearHumidityFilter.setPredicates(List.of(clearHumidityFilterPredicate));
        clearHumidityCondition.setExpression(new SimpleAlarmConditionExpression(List.of(clearHumidityFilter), ComplexOperation.AND));
        clearHumidityRule.setCondition(clearHumidityCondition);
        clearHumidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidityConfig.setClearRule(clearHumidityRule);

        calculatedFieldService.save(lowHumidity);
    }

    @Override
    public void loadSystemWidgets() throws Exception {
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
        user = userService.saveUser(tenantId, user);
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
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        save(device.getId(), ACTIVITY_STATE, false);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(TenantId.SYS_TENANT_ID, deviceCredentials);
        return device;
    }

    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistActivityToTelemetry) {
            ListenableFuture<TimeseriesSaveResult> saveFuture = tsService.save(
                    TenantId.SYS_TENANT_ID,
                    deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))), 0L);
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            ListenableFuture<AttributesSaveResult> saveFuture = attributesService.save(
                    TenantId.SYS_TENANT_ID, deviceId, AttributeScope.SERVER_SCOPE, new BaseAttributeKvEntry(new BooleanDataEntry(key, value), System.currentTimeMillis())
            );
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        private final DeviceId deviceId;
        private final String key;
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }

    }

    private <S> void addTsCallback(ListenableFuture<S> saveFuture, final FutureCallback<S> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    @Override
    public void createQueues() {
        Queue mainQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.MAIN_QUEUE_NAME);
        if (mainQueue == null) {
            mainQueue = new Queue();
            mainQueue.setTenantId(TenantId.SYS_TENANT_ID);
            mainQueue.setName(DataConstants.MAIN_QUEUE_NAME);
            mainQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
            mainQueue.setPollInterval(25);
            mainQueue.setPartitions(10);
            mainQueue.setConsumerPerPartition(true);
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
            queueService.saveQueue(mainQueue);
        }

        Queue highPriorityQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.HP_QUEUE_NAME);
        if (highPriorityQueue == null) {
            highPriorityQueue = new Queue();
            highPriorityQueue.setTenantId(TenantId.SYS_TENANT_ID);
            highPriorityQueue.setName(DataConstants.HP_QUEUE_NAME);
            highPriorityQueue.setTopic(DataConstants.HP_QUEUE_TOPIC);
            highPriorityQueue.setPollInterval(25);
            highPriorityQueue.setPartitions(10);
            highPriorityQueue.setConsumerPerPartition(true);
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
            queueService.saveQueue(highPriorityQueue);
        }

        Queue sequentialByOriginatorQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.SQ_QUEUE_NAME);
        if (sequentialByOriginatorQueue == null) {
            sequentialByOriginatorQueue = new Queue();
            sequentialByOriginatorQueue.setTenantId(TenantId.SYS_TENANT_ID);
            sequentialByOriginatorQueue.setName(DataConstants.SQ_QUEUE_NAME);
            sequentialByOriginatorQueue.setTopic(DataConstants.SQ_QUEUE_TOPIC);
            sequentialByOriginatorQueue.setPollInterval(25);
            sequentialByOriginatorQueue.setPartitions(10);
            sequentialByOriginatorQueue.setPackProcessingTimeout(2000);
            sequentialByOriginatorQueue.setConsumerPerPartition(true);
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
            queueService.saveQueue(sequentialByOriginatorQueue);
        }
    }

    @Override
    @SneakyThrows
    public void createDefaultNotificationConfigs() {
        log.info("Creating default notification configs for system admin");
        if (notificationTargetService.countNotificationTargetsByTenantId(TenantId.SYS_TENANT_ID) == 0) {
            notificationSettingsService.createDefaultNotificationConfigs(TenantId.SYS_TENANT_ID);
        }
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 500);
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
        log.info("Creating default notification configs for all tenants");
        AtomicInteger count = new AtomicInteger();
        for (TenantId tenantId : tenants) {
            executor.submit(() -> {
                if (notificationTargetService.countNotificationTargetsByTenantId(tenantId) == 0) {
                    notificationSettingsService.createDefaultNotificationConfigs(tenantId);
                    int n = count.incrementAndGet();
                    if (n % 500 == 0) {
                        log.info("{} tenants processed", n);
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public void updateDefaultNotificationConfigs(boolean updateTenants) {
        log.info("Updating notification configs...");
        notificationSettingsService.updateDefaultNotificationConfigs(TenantId.SYS_TENANT_ID);

        if (updateTenants) {
            PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 500);
            ExecutorService executor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
            AtomicInteger count = new AtomicInteger();
            for (TenantId tenantId : tenants) {
                executor.submit(() -> {
                    notificationSettingsService.updateDefaultNotificationConfigs(tenantId);
                    int n = count.incrementAndGet();
                    if (n % 500 == 0) {
                        log.info("{} tenants processed", n);
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        }
    }

}
