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
package org.thingsboard.rule.engine.profile;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Data
class DeviceProfileAlarmState {

    private final EntityId originator;
    private final DeviceProfileAlarm alarmDefinition;
    private volatile Map<AlarmSeverity, AlarmRule> createRulesSortedBySeverityDesc;
    private volatile Alarm currentAlarm;

    public DeviceProfileAlarmState(EntityId originator, DeviceProfileAlarm alarmDefinition) {
        this.originator = originator;
        this.alarmDefinition = alarmDefinition;
        this.createRulesSortedBySeverityDesc = new TreeMap<>(Comparator.comparingInt(AlarmSeverity::ordinal));
        this.createRulesSortedBySeverityDesc.putAll(alarmDefinition.getCreateRules());
    }

    public void process(TbContext ctx, TbMsg msg, DeviceDataSnapshot data) {
        AlarmSeverity resultSeverity = null;
        for (Map.Entry<AlarmSeverity, AlarmRule> kv : createRulesSortedBySeverityDesc.entrySet()) {
            AlarmRule alarmRule = kv.getValue();
            if (eval(alarmRule.getCondition(), data)) {
                resultSeverity = kv.getKey();
                break;
            }
        }
        if (resultSeverity != null) {
            pushMsg(ctx, calculateAlarmResult(ctx, resultSeverity), msg);
        } else if (currentAlarm != null) {
            AlarmRule clearRule = alarmDefinition.getClearRule();
            if (eval(clearRule.getCondition(), data)) {
                pushMsg(ctx, new TbAlarmResult(false, false, true, currentAlarm), msg);
                currentAlarm = null;
            }
        }
    }

    public void pushMsg(TbContext ctx, TbAlarmResult alarmResult, TbMsg originalMsg) {
        JsonNode jsonNodes = JacksonUtil.valueToTree(alarmResult.getAlarm());
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        String relationType;
        if (alarmResult.isCreated()) {
            relationType = "Alarm Created";
            metaData.putValue(DataConstants.IS_NEW_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isUpdated()) {
            relationType = "Alarm Updated";
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
        } else if (alarmResult.isSeverityUpdated()) {
            relationType = "Alarm Severity Updated";
            metaData.putValue(DataConstants.IS_EXISTING_ALARM, Boolean.TRUE.toString());
            metaData.putValue(DataConstants.IS_SEVERITY_UPDATED_ALARM, Boolean.TRUE.toString());
        } else {
            relationType = "Alarm Cleared";
            metaData.putValue(DataConstants.IS_CLEARED_ALARM, Boolean.TRUE.toString());
        }
        TbMsg newMsg = ctx.newMsg(originalMsg.getQueueName(), "ALARM", originalMsg.getOriginator(), metaData, data);
        ctx.tellNext(newMsg, relationType);
    }

    private TbAlarmResult calculateAlarmResult(TbContext ctx, AlarmSeverity severity) {
        if (currentAlarm != null) {
            currentAlarm.setEndTs(System.currentTimeMillis());
            AlarmSeverity oldSeverity = currentAlarm.getSeverity();
            if (!oldSeverity.equals(severity)) {
                currentAlarm.setSeverity(severity);
                currentAlarm = ctx.getAlarmService().createOrUpdateAlarm(currentAlarm);
                return new TbAlarmResult(false, false, true, false, currentAlarm);
            } else {
                currentAlarm = ctx.getAlarmService().createOrUpdateAlarm(currentAlarm);
                return new TbAlarmResult(false, true, false, false, currentAlarm);
            }
        } else {
            currentAlarm = new Alarm();
            currentAlarm.setSeverity(severity);
            currentAlarm.setStartTs(System.currentTimeMillis());
            currentAlarm.setEndTs(currentAlarm.getStartTs());
            currentAlarm.setDetails(JacksonUtil.OBJECT_MAPPER.createObjectNode());
            currentAlarm.setOriginator(originator);
            currentAlarm.setTenantId(ctx.getTenantId());
            currentAlarm.setPropagate(alarmDefinition.isPropagate());
            if (alarmDefinition.getPropagateRelationTypes() != null) {
                currentAlarm.setPropagateRelationTypes(alarmDefinition.getPropagateRelationTypes());
            }
            currentAlarm = ctx.getAlarmService().createOrUpdateAlarm(currentAlarm);
            boolean updated = currentAlarm.getStartTs() != currentAlarm.getEndTs();
            return new TbAlarmResult(!updated, updated, false, false, currentAlarm);
        }
    }

    private boolean eval(AlarmCondition condition, DeviceDataSnapshot data) {
        boolean eval = true;
        for (KeyFilter keyFilter : condition.getCondition()) {
            EntityKeyValue value = data.getValue(keyFilter.getKey());
            if (value == null) {
                return false;
            }
            eval = eval && eval(value, keyFilter.getPredicate());
        }
        //TODO: use condition duration;
        return eval;
    }

    private boolean eval(EntityKeyValue value, KeyFilterPredicate predicate) {
        switch (predicate.getType()) {
            case STRING:
                return evalStrPredicate(value, (StringFilterPredicate) predicate);
            case NUMERIC:
                return evalNumPredicate(value, (NumericFilterPredicate) predicate);
            case COMPLEX:
                return evalComplexPredicate(value, (ComplexFilterPredicate) predicate);
            case BOOLEAN:
                return evalBoolPredicate(value, (BooleanFilterPredicate) predicate);
            default:
                return false;
        }
    }

    private boolean evalComplexPredicate(EntityKeyValue ekv, ComplexFilterPredicate predicate) {
        switch (predicate.getOperation()) {
            case OR:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (eval(ekv, kfp)) {
                        return true;
                    }
                }
                return false;
            case AND:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (!eval(ekv, kfp)) {
                        return false;
                    }
                }
                return true;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }


    private boolean evalBoolPredicate(EntityKeyValue ekv, BooleanFilterPredicate predicate) {
        Boolean value;
        switch (ekv.getDataType()) {
            case LONG:
                value = ekv.getLngValue() > 0;
                break;
            case DOUBLE:
                value = ekv.getDblValue() > 0;
                break;
            case BOOLEAN:
                value = ekv.getBoolValue();
                break;
            case STRING:
                try {
                    value = Boolean.parseBoolean(ekv.getStrValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            case JSON:
                try {
                    value = Boolean.parseBoolean(ekv.getJsonValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            default:
                return false;
        }
        if (value == null) {
            return false;
        }
        switch (predicate.getOperation()) {
            case EQUAL:
                return value.equals(predicate.getValue().getDefaultValue());
            case NOT_EQUAL:
                return !value.equals(predicate.getValue().getDefaultValue());
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalNumPredicate(EntityKeyValue ekv, NumericFilterPredicate predicate) {
        Double value;
        switch (ekv.getDataType()) {
            case LONG:
                value = ekv.getLngValue().doubleValue();
                break;
            case DOUBLE:
                value = ekv.getDblValue();
                break;
            case BOOLEAN:
                value = ekv.getBoolValue() ? 1.0 : 0.0;
                break;
            case STRING:
                try {
                    value = Double.parseDouble(ekv.getStrValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            case JSON:
                try {
                    value = Double.parseDouble(ekv.getJsonValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            default:
                return false;
        }
        if (value == null) {
            return false;
        }

        Double predicateValue = predicate.getValue().getDefaultValue();
        switch (predicate.getOperation()) {
            case NOT_EQUAL:
                return !value.equals(predicateValue);
            case EQUAL:
                return value.equals(predicateValue);
            case GREATER:
                return value > predicateValue;
            case GREATER_OR_EQUAL:
                return value >= predicateValue;
            case LESS:
                return value < predicateValue;
            case LESS_OR_EQUAL:
                return value <= predicateValue;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalStrPredicate(EntityKeyValue ekv, StringFilterPredicate predicate) {
        String val;
        String predicateValue;
        if (predicate.isIgnoreCase()) {
            val = ekv.getStrValue().toLowerCase();
            predicateValue = predicate.getValue().getDefaultValue().toLowerCase();
        } else {
            val = ekv.getStrValue();
            predicateValue = predicate.getValue().getDefaultValue();
        }
        switch (predicate.getOperation()) {
            case CONTAINS:
                return val.contains(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case STARTS_WITH:
                return val.startsWith(predicateValue);
            case ENDS_WITH:
                return val.endsWith(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case NOT_CONTAINS:
                return !val.contains(predicateValue);
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }
}
