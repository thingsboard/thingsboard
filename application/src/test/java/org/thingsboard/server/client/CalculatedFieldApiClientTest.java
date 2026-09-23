// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.DeleteCalculatedFieldArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCalculatedFieldByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCalculatedFieldsByEntityIdArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveCalculatedFieldArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.model.AlarmCalculatedFieldConfiguration;
import org.thingsboard.client.model.AlarmConditionValueAlarmSchedule;
import org.thingsboard.client.model.AlarmRule;
import org.thingsboard.client.model.AlarmSeverity;
import org.thingsboard.client.model.Argument;
import org.thingsboard.client.model.ArgumentType;
import org.thingsboard.client.model.CalculatedField;
import org.thingsboard.client.model.CalculatedFieldType;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.PageDataCalculatedField;
import org.thingsboard.client.model.ReferencedEntityKey;
import org.thingsboard.client.model.SimpleAlarmCondition;
import org.thingsboard.client.model.SimpleCalculatedFieldConfiguration;
import org.thingsboard.client.model.SpecificTimeSchedule;
import org.thingsboard.client.model.TbelAlarmConditionExpression;
import org.thingsboard.client.model.TimeSeriesOutput;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class CalculatedFieldApiClientTest extends AbstractApiClientTest {

    @Test
    public void testCalculatedFieldLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<CalculatedField> createdFields = new ArrayList<>();

        // create devices to attach calculated fields to
        Device device1 = new Device();
        device1.setName("CalcFieldDevice1_" + timestamp);
        device1.setType("default");
        Device createdDevice1 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device1)
                .build());

        Device device2 = new Device();
        device2.setName("CalcFieldDevice2_" + timestamp);
        device2.setType("default");
        Device createdDevice2 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device2)
                .build());

        // create calculated fields on device1
        for (int i = 0; i < 5; i++) {
            CalculatedField cf = new CalculatedField();
            cf.setName(TEST_PREFIX + "CalcField_" + timestamp + "_" + i);
            cf.setType(CalculatedFieldType.SIMPLE);

            cf.setEntityId(createdDevice1.getId());

            SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

            Argument arg = new Argument();
            ReferencedEntityKey refKey = new ReferencedEntityKey();
            refKey.setKey("temperature");
            refKey.setType(ArgumentType.TS_LATEST);
            arg.setRefEntityKey(refKey);
            config.putArgumentsItem("temp", arg);

            config.setExpression("temp * " + (i + 1));

            TimeSeriesOutput output = new TimeSeriesOutput();
            output.setName("scaledTemp_" + i);
            config.setOutput(output);

            cf.setConfiguration(config);

            CalculatedField created = client.saveCalculatedField(SaveCalculatedFieldArgs.builder()
                    .calculatedField(cf)
                    .build());
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals(cf.getName(), created.getName());
            assertEquals(CalculatedFieldType.SIMPLE, created.getType());

            createdFields.add(created);
        }

        // create calculated fields on device2
        for (int i = 0; i < 3; i++) {
            CalculatedField cf = new CalculatedField();
            cf.setName(TEST_PREFIX + "CalcField2_" + timestamp + "_" + i);
            cf.setType(CalculatedFieldType.SIMPLE);
            cf.setEntityId(createdDevice2.getId());

            SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

            Argument arg = new Argument();
            ReferencedEntityKey refKey = new ReferencedEntityKey();
            refKey.setKey("humidity");
            refKey.setType(ArgumentType.TS_LATEST);
            arg.setRefEntityKey(refKey);
            config.putArgumentsItem("hum", arg);

            config.setExpression("hum + " + i);

            TimeSeriesOutput output = new TimeSeriesOutput();
            output.setName("adjustedHumidity_" + i);
            config.setOutput(output);

            cf.setConfiguration(config);

            CalculatedField created = client.saveCalculatedField(SaveCalculatedFieldArgs.builder()
                    .calculatedField(cf)
                    .build());
            assertNotNull(created);
            createdFields.add(created);
        }

        // get calculated fields by entity id for device1
        PageDataCalculatedField device1Fields = client.getCalculatedFieldsByEntityId(GetCalculatedFieldsByEntityIdArgs.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice1.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .type(CalculatedFieldType.SIMPLE)
                .build());
        assertNotNull(device1Fields);
        assertEquals(5, device1Fields.getData().size());

        // get calculated fields by entity id for device2
        PageDataCalculatedField device2Fields = client.getCalculatedFieldsByEntityId(GetCalculatedFieldsByEntityIdArgs.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice2.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .type(CalculatedFieldType.SIMPLE)
                .build());
        assertEquals(3, device2Fields.getData().size());

        // get by id
        CalculatedField searchField = createdFields.get(2);
        CalculatedField fetchedField = client.getCalculatedFieldById(GetCalculatedFieldByIdArgs.builder()
                .calculatedFieldId(searchField.getId().getId().toString())
                .build());
        assertEquals(searchField.getName(), fetchedField.getName());
        assertEquals(searchField.getType(), fetchedField.getType());
        assertNotNull(fetchedField.getConfiguration());
        SimpleCalculatedFieldConfiguration fetchedConfig =
                (SimpleCalculatedFieldConfiguration) fetchedField.getConfiguration();
        assertEquals("temp * 3", fetchedConfig.getExpression());

        // update calculated field
        fetchedField.setName(fetchedField.getName() + "_updated");
        fetchedConfig.setExpression("temp * 100");
        CalculatedField updatedField = client.saveCalculatedField(SaveCalculatedFieldArgs.builder()
                .calculatedField(fetchedField)
                .build());
        assertEquals(fetchedField.getName(), updatedField.getName());
        SimpleCalculatedFieldConfiguration updatedConfig =
                (SimpleCalculatedFieldConfiguration) updatedField.getConfiguration();
        assertEquals("temp * 100", updatedConfig.getExpression());

        // delete calculated field
        UUID fieldToDeleteId = createdFields.get(0).getId().getId();
        client.deleteCalculatedField(DeleteCalculatedFieldArgs.builder()
                .calculatedFieldId(fieldToDeleteId.toString())
                .build());

        // verify deletion
        assertReturns404(() ->
                client.getCalculatedFieldById(GetCalculatedFieldByIdArgs.builder()
                        .calculatedFieldId(fieldToDeleteId.toString())
                        .build())
        );

        PageDataCalculatedField device1FieldsAfterDelete = client.getCalculatedFieldsByEntityId(GetCalculatedFieldsByEntityIdArgs.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice1.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(4, device1FieldsAfterDelete.getData().size());
    }

    @Test
    public void testAlarmCalculatedFieldLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();

        // create a device to attach the alarm calculated field to
        Device device = new Device();
        device.setName("AlarmCalcFieldDevice_" + timestamp);
        device.setType("default");
        Device createdDevice = client.saveDevice(SaveDeviceArgs.builder()
                .device(device)
                .build());

        // build the alarm calculated field configuration
        AlarmCalculatedFieldConfiguration config = new AlarmCalculatedFieldConfiguration();

        // argument: temperature time-series
        Argument tempArg = new Argument();
        ReferencedEntityKey refKey = new ReferencedEntityKey();
        refKey.setKey("temperature");
        refKey.setType(ArgumentType.TS_LATEST);
        tempArg.setRefEntityKey(refKey);
        config.putArgumentsItem("temp", tempArg);

        // create rule: HIGH_TEMPERATURE when temp > 50 (TBEL expression)
        TbelAlarmConditionExpression createExpression = new TbelAlarmConditionExpression();
        createExpression.setExpression("return temp > 50;");
        SimpleAlarmCondition createCondition = new SimpleAlarmCondition();
        createCondition.setExpression(createExpression);
        SpecificTimeSchedule specificTimeSchedule = new SpecificTimeSchedule().addDaysOfWeekItem(3);
        AlarmConditionValueAlarmSchedule schedule = new AlarmConditionValueAlarmSchedule().staticValue(specificTimeSchedule);
        createCondition.setSchedule(schedule);
        AlarmRule createRule = new AlarmRule();
        createRule.setCondition(createCondition);
        createRule.setAlarmDetails("Temperature is too high: ${temp}");
        config.setCreateRules(Map.of(
                AlarmSeverity.CRITICAL.name(), createRule
        ));

        // clear rule: when temp drops below 30
        TbelAlarmConditionExpression clearExpression = new TbelAlarmConditionExpression();
        clearExpression.setExpression("return temp < 30;");
        SimpleAlarmCondition clearCondition = new SimpleAlarmCondition();
        clearCondition.setExpression(clearExpression);
        AlarmRule clearRule = new AlarmRule();
        clearRule.setCondition(clearCondition);
        config.setClearRule(clearRule);

        config.setPropagate(true);
        config.setPropagateToOwner(false);

        // create calculated field
        CalculatedField cf = new CalculatedField();
        cf.setName(TEST_PREFIX + "AlarmCalcField_" + timestamp);
        cf.setType(CalculatedFieldType.ALARM);

        cf.setEntityId(createdDevice.getId());
        cf.setConfiguration(config);

        CalculatedField created = client.saveCalculatedField(SaveCalculatedFieldArgs.builder()
                .calculatedField(cf)
                .build());
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(cf.getName(), created.getName());
        assertEquals(CalculatedFieldType.ALARM, created.getType());
        AlarmCalculatedFieldConfiguration configuration = (AlarmCalculatedFieldConfiguration) created.getConfiguration();
        AlarmConditionValueAlarmSchedule createdSchedule = configuration.getCreateRules().get(AlarmSeverity.CRITICAL.name()).getCondition().getSchedule();
        SpecificTimeSchedule staticSchedule = (SpecificTimeSchedule) createdSchedule.getStaticValue();
        assertEquals(Set.of(3), staticSchedule.getDaysOfWeek());

        // get by id and verify configuration
        CalculatedField fetched = client.getCalculatedFieldById(GetCalculatedFieldByIdArgs.builder()
                .calculatedFieldId(created.getId().getId().toString())
                .build());
        assertNotNull(fetched);
        assertEquals(created.getName(), fetched.getName());
        assertEquals(CalculatedFieldType.ALARM, fetched.getType());
        assertNotNull(fetched.getConfiguration());
        AlarmCalculatedFieldConfiguration fetchedConfig =
                (AlarmCalculatedFieldConfiguration) fetched.getConfiguration();
        assertNotNull(fetchedConfig.getCreateRules());
        assertEquals(1, fetchedConfig.getCreateRules().size());
        assertTrue(fetchedConfig.getCreateRules().containsKey("CRITICAL"));
        assertNotNull(fetchedConfig.getClearRule());
        assertEquals(Boolean.TRUE, fetchedConfig.getPropagate());

        // update: add a second create rule for CRITICAL_TEMPERATURE
        TbelAlarmConditionExpression criticalExpression = new TbelAlarmConditionExpression();
        criticalExpression.setExpression("return temp > 80;");
        SimpleAlarmCondition criticalCondition = new SimpleAlarmCondition();
        criticalCondition.setExpression(criticalExpression);
        AlarmRule criticalRule = new AlarmRule();
        criticalRule.setCondition(criticalCondition);
        fetchedConfig.putCreateRulesItem(AlarmSeverity.INDETERMINATE.name(), criticalRule);
        fetched.setConfiguration(fetchedConfig);

        CalculatedField updated = client.saveCalculatedField(SaveCalculatedFieldArgs.builder()
                .calculatedField(fetched)
                .build());
        AlarmCalculatedFieldConfiguration updatedConfig =
                (AlarmCalculatedFieldConfiguration) updated.getConfiguration();
        assertEquals(2, updatedConfig.getCreateRules().size());
        assertTrue(updatedConfig.getCreateRules().containsKey("INDETERMINATE"));

        // filter by entity and ALARM type
        PageDataCalculatedField deviceFields = client.getCalculatedFieldsByEntityId(GetCalculatedFieldsByEntityIdArgs.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .type(CalculatedFieldType.ALARM)
                .build());
        assertNotNull(deviceFields);
        assertEquals(1, deviceFields.getData().size());

        // delete and verify
        UUID fieldId = created.getId().getId();
        client.deleteCalculatedField(DeleteCalculatedFieldArgs.builder()
                .calculatedFieldId(fieldId.toString())
                .build());
        assertReturns404(() -> client.getCalculatedFieldById(GetCalculatedFieldByIdArgs.builder()
                .calculatedFieldId(fieldId.toString())
                .build()));
    }

}
