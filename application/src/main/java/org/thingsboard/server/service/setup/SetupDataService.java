// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.setup;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionFilter;
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
import org.thingsboard.server.common.data.group.EntityGroup;
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
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.ComplexOperation;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.thingsboard.server.common.data.DataConstants.DEFAULT_DEVICE_TYPE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;

@Service
@Slf4j
@RequiredArgsConstructor
public class SetupDataService {

    private static final String CUSTOMER_CRED = "customer";

    private final InstallScripts installScripts;
    private final UserService userService;
    private final TenantService tenantService;
    private final CustomerService customerService;
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    private final AttributesService attributesService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final RuleChainService ruleChainService;
    private final TimeseriesService tsService;
    private final CalculatedFieldService calculatedFieldService;
    private final EntityGroupService entityGroupService;
    private final SystemSecurityService systemSecurityService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistActivityToTelemetry;

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("setup-data-ts-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    /**
     * Transactional because it is two writes: {@code saveUser} creates the user with DISABLED credentials, and
     * only the second sets the password and enables them. A failure in between would leave a SYS_ADMIN nobody
     * can sign in as, while its existence makes the setup report the account step as done from then on.
     */
    @Transactional
    public void createSysAdmin(String email, String password) {
        // The password comes from an unauthenticated caller, so it must satisfy the same password policy as
        // any other password accepted from a user.
        systemSecurityService.validatePassword(password, null);
        createUser(Authority.SYS_ADMIN, null, null, email, password);
    }

    public void loadDemoData() throws Exception {
        Tenant demoTenant = new Tenant();
        demoTenant.setRegion("Global");
        demoTenant.setTitle("Tenant");
        demoTenant = tenantService.saveTenant(demoTenant, tenantId -> {
            entityGroupService.createDefaultTenantEntityGroups(tenantId);
            installScripts.loadDemoRuleChains(tenantId);
        });
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

        installScripts.createSystemNotificationTemplates(demoTenant.getId());
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
        if (Authority.TENANT_ADMIN.equals(authority)) {
            EntityGroup admins = entityGroupService.findOrCreateTenantAdminsGroup(user.getTenantId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, admins.getId(), user.getId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            EntityGroup users = entityGroupService.findOrCreateCustomerUsersGroup(user.getTenantId(), user.getCustomerId(), null);
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, users.getId(), user.getId());
        }
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
        device.setDeviceProfileId(deviceProfileId);
        device.setName(name);
        if (description != null) {
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        save(device.getId(), ACTIVITY_STATE, false);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        if (customerId != null && !customerId.isNullUid()) {
            EntityGroup deviceGroup = entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DEVICE);
            entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());
        }
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
        Futures.addCallback(saveFuture, callback, tsCallBackExecutor);
    }

}
