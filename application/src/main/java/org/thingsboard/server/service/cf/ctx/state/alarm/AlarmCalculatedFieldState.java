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
package org.thingsboard.server.service.cf.ctx.state.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.ComplexOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.SimpleAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.BooleanFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.ComplexFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.KeyFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.StringFilterPredicate;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.service.cf.AlarmCalculatedFieldResult;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static org.thingsboard.server.common.data.StringUtils.equalsAny;
import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AlarmCalculatedFieldState extends BaseCalculatedFieldState {

    private String alarmType;
    private AlarmCalculatedFieldConfiguration configuration;

    @Getter
    private final Map<AlarmSeverity, AlarmRuleState> createRuleStates = new TreeMap<>(Comparator.comparing(Enum::ordinal));
    @Getter
    private AlarmRuleState clearRuleState;

    @Getter
    private Alarm currentAlarm;
    private boolean initialFetchDone;

    public AlarmCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void init(CalculatedFieldCtx ctx) {
        super.init(ctx);

        this.alarmType = ctx.getCalculatedField().getName();
        this.configuration = getConfiguration(ctx);

        Map<AlarmSeverity, AlarmRule> createRules = configuration.getCreateRules();
        createRules.forEach((severity, rule) -> {
            AlarmRuleState ruleState = createRuleStates.get(severity);
            if (ruleState == null) {
                ruleState = new AlarmRuleState(severity, rule, this);
                createRuleStates.put(severity, ruleState);
            } else { // can be null if was restored
                ruleState.setAlarmRule(rule);
                // todo: is it enough to just set new alarm rule to alarm rule state? is it ok to leave the state as were??
            }
        });
        createRuleStates.keySet().removeIf(severity -> !createRules.containsKey(severity));

        AlarmRule clearRule = configuration.getClearRule();
        if (clearRule != null) {
            if (clearRuleState == null) {
                clearRuleState = new AlarmRuleState(null, clearRule, this);
            } else {
                clearRuleState.setAlarmRule(clearRule);
            }
        } else {
            clearRuleState = null;
        }
        log.debug("Initialized create rule states {} and clear rule state {} for {}", createRuleStates, clearRuleState, ctx.getCalculatedField());
    }

    @Override
    public Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> argumentValues, CalculatedFieldCtx ctx) {
        return super.update(argumentValues, ctx);
    }

    @Override
    public void reset(CalculatedFieldCtx ctx) {
        super.reset(ctx);
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        initCurrentAlarm(ctx);
        TbAlarmResult result = createOrClearAlarms(state -> {
            if (updatedArgs != null) {
                boolean newEvent = !updatedArgs.isEmpty();
                return state.eval(newEvent, ctx);
            } else {
                return state.eval(System.currentTimeMillis());
            }
        }, ctx);
        return Futures.immediateFuture(AlarmCalculatedFieldResult.builder()
                .alarmResult(result)
                .build());
    }

    public void processAlarmAction(Alarm alarm, ActionType action) {
        switch (action) {
            case ALARM_ACK -> processAlarmAck(alarm);
            case ALARM_CLEAR -> processAlarmClear(alarm);
            case ALARM_DELETE -> processAlarmDelete(alarm);
        }
    }

    private void processAlarmClear(Alarm alarm) {
        currentAlarm = null;
        createRuleStates.values().forEach(AlarmRuleState::clear);
    }

    private void processAlarmAck(Alarm alarm) {
        currentAlarm.setAcknowledged(alarm.isAcknowledged());
        currentAlarm.setAckTs(alarm.getAckTs());
    }

    private void processAlarmDelete(Alarm alarm) {
        currentAlarm = null;
        createRuleStates.values().forEach(AlarmRuleState::clear);
    }

    private TbAlarmResult createOrClearAlarms(Function<AlarmRuleState, AlarmEvalResult> evalFunction,
                                              CalculatedFieldCtx ctx) {
        TbAlarmResult result = null;
        AlarmRuleState resultState = null;
        AlarmRuleState.StateInfo resultStateInfo = null;

        for (AlarmRuleState state : createRuleStates.values()) {
            AlarmEvalResult evalResult = evalFunction.apply(state);
            log.debug("Evaluated create rule {} with args {}. Result: {}", state, arguments, evalResult);
            if (evalResult == AlarmEvalResult.TRUE) {
                resultState = state;
                break;
            } else if (evalResult == AlarmEvalResult.FALSE) {
                clearAlarmState(state);
            }
        }

        if (resultState != null) {
            result = calculateAlarmResult(resultState, ctx);
            resultStateInfo = resultState.getStateInfo();
            log.debug("Alarm result for state {}: {}", resultState, result);
            clearAlarmState(clearRuleState);
        } else if (currentAlarm != null && clearRuleState != null) {
            AlarmEvalResult evalResult = evalFunction.apply(clearRuleState);
            log.debug("Evaluated clear rule {} with args {}. Result: {}", clearRuleState, arguments, evalResult);
            if (evalResult == AlarmEvalResult.TRUE) {
                resultStateInfo = clearRuleState.getStateInfo();
                clearAlarmState(clearRuleState);
                for (AlarmRuleState state : createRuleStates.values()) {
                    clearAlarmState(state);
                }
                AlarmApiCallResult clearResult = ctx.getAlarmService().clearAlarm(
                        ctx.getTenantId(), currentAlarm.getId(), System.currentTimeMillis(), createDetails(clearRuleState), true
                );
                if (clearResult.isCleared()) {
                    result = TbAlarmResult.builder()
                            .isCleared(true)
                            .alarm(clearResult.getAlarm())
                            .build();
                    addStateInfo(result, clearRuleState);
                    resultState = clearRuleState;
                }
                currentAlarm = null;
            } else if (evalResult == AlarmEvalResult.FALSE) {
                clearAlarmState(clearRuleState);
            }
        }
        if (result != null && resultState != null) {
            result.setConditionRepeats(resultStateInfo.eventCount());
            result.setConditionDuration(resultStateInfo.duration());
        }
        return result;
    }

    private void clearAlarmState(AlarmRuleState state) {
        if (state != null) {
            state.clear();
        }
    }

    private void initCurrentAlarm(CalculatedFieldCtx ctx) {
        if (!initialFetchDone) {
            Alarm alarm = ctx.getAlarmService().findLatestActiveByOriginatorAndType(ctx.getTenantId(), entityId, alarmType);
            if (alarm != null && !alarm.getStatus().isCleared()) {
                currentAlarm = alarm;
            }
            initialFetchDone = true;
        }
    }

    private TbAlarmResult calculateAlarmResult(AlarmRuleState ruleState, CalculatedFieldCtx ctx) {
        AlarmSeverity severity = ruleState.getSeverity();
        if (currentAlarm != null) {
            // TODO: In some extremely rare cases, we might miss the event of alarm clear (If one use in-mem queue and restarted the server) or (if one manipulated the rule chain).
            // Maybe we should fetch alarm every time?
            currentAlarm.setEndTs(System.currentTimeMillis());
            AlarmSeverity oldSeverity = currentAlarm.getSeverity();
            // Skip update if severity is decreased.
            if (severity.ordinal() <= oldSeverity.ordinal()) {
                currentAlarm.setDetails(createDetails(ruleState));
                currentAlarm.setSeverity(severity);
                AlarmApiCallResult result = ctx.getAlarmService().updateAlarm(AlarmUpdateRequest.fromAlarm(currentAlarm));
                currentAlarm = result.getAlarm();
                return TbAlarmResult.fromAlarmResult(result);
            } else {
                return null;
            }
        } else {
            var newAlarm = new Alarm();
            newAlarm.setType(alarmType);
            newAlarm.setAcknowledged(false);
            newAlarm.setCleared(false);
            newAlarm.setSeverity(severity);
            long startTs = latestTimestamp;
            long currentTime = System.currentTimeMillis();
            if (startTs == 0L || startTs > currentTime) {
                startTs = currentTime;
            }
            newAlarm.setStartTs(startTs);
            newAlarm.setEndTs(startTs);
            newAlarm.setDetails(createDetails(ruleState));
            newAlarm.setOriginator(entityId);
            newAlarm.setTenantId(ctx.getTenantId());
            newAlarm.setPropagate(configuration.isPropagate());
            newAlarm.setPropagateToOwner(configuration.isPropagateToOwner());
            newAlarm.setPropagateToTenant(configuration.isPropagateToTenant());
            if (configuration.getPropagateRelationTypes() != null) {
                newAlarm.setPropagateRelationTypes(configuration.getPropagateRelationTypes());
            }
            AlarmApiCallResult result = ctx.getAlarmService().createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(newAlarm));
            currentAlarm = result.getAlarm();
            return TbAlarmResult.fromAlarmResult(result);
        }
    }

    private void addStateInfo(TbAlarmResult alarmResult, AlarmRuleState ruleState) {
        if (ruleState.getCondition().getType() == AlarmConditionType.REPEATING) {
            alarmResult.setConditionRepeats(ruleState.getEventCount());
        } else if (ruleState.getCondition().getType() == AlarmConditionType.DURATION) {
            alarmResult.setConditionDuration(ruleState.getDuration());
        }
    }

    private JsonNode createDetails(AlarmRuleState ruleState) {
        JsonNode alarmDetails;
        String alarmDetailsStr = ruleState.getAlarmRule().getAlarmDetails();
        DashboardId dashboardId = ruleState.getAlarmRule().getDashboardId();

        if (StringUtils.isNotEmpty(alarmDetailsStr) || dashboardId != null) {
            ObjectNode newDetails = JacksonUtil.newObjectNode();
            if (StringUtils.isNotEmpty(alarmDetailsStr)) {
                for (Map.Entry<String, ArgumentEntry> entry : arguments.entrySet()) {
                    String key = entry.getKey();
                    ArgumentEntry value = entry.getValue();
                    alarmDetailsStr = alarmDetailsStr.replaceAll(String.format("\\$\\{%s}", key), String.valueOf(value.getValue()));
                }
                newDetails.put("data", alarmDetailsStr);
            }
            if (dashboardId != null) {
                newDetails.put("dashboardId", dashboardId.getId().toString());
            }
            alarmDetails = newDetails;
        } else if (currentAlarm != null) {
            alarmDetails = currentAlarm.getDetails();
        } else {
            alarmDetails = JacksonUtil.newObjectNode();
        }

        return alarmDetails;
    }

    @SneakyThrows
    public boolean eval(AlarmConditionExpression expression, CalculatedFieldCtx ctx) {
        if (expression instanceof TbelAlarmConditionExpression tbelExpression) {
            Object result = ctx.evaluateTbelExpression(tbelExpression.getExpression(), this).get();
            if (result instanceof Boolean booleanResult) {
                return booleanResult;
            } else {
                throw new IllegalStateException("Condition expression returned non-boolean value: '" + result + "'");
            }
        } else {
            SimpleAlarmConditionExpression simpleExpression = (SimpleAlarmConditionExpression) expression;
            ComplexOperation operation = simpleExpression.getOperation();
            if (operation == null) {
                operation = ComplexOperation.AND;
            }
            return switch (operation) {
                case OR -> {
                    for (AlarmConditionFilter filter : simpleExpression.getFilters()) {
                        SingleValueArgumentEntry argument = getArgument(filter.getArgument());
                        if (eval(argument, filter.getPredicate())) {
                            yield true;
                        }
                    }
                    yield false;
                }
                case AND -> {
                    for (AlarmConditionFilter filter : simpleExpression.getFilters()) {
                        SingleValueArgumentEntry argument = getArgument(filter.getArgument());
                        if (!eval(argument, filter.getPredicate())) {
                            yield false;
                        }
                    }
                    yield true;
                }
            };
        }
    }

    private boolean eval(SingleValueArgumentEntry argument, KeyFilterPredicate predicate) {
        return switch (predicate.getType()) {
            case STRING -> evalStrPredicate(argument, (StringFilterPredicate) predicate);
            case NUMERIC -> evalNumPredicate(argument, (NumericFilterPredicate) predicate);
            case BOOLEAN -> evalBooleanPredicate(argument, (BooleanFilterPredicate) predicate);
            case COMPLEX -> evalComplexPredicate(argument, (ComplexFilterPredicate) predicate);
        };
    }

    private boolean evalComplexPredicate(SingleValueArgumentEntry argument, ComplexFilterPredicate complexPredicate) {
        return switch (complexPredicate.getOperation()) {
            case OR -> {
                for (KeyFilterPredicate predicate : complexPredicate.getPredicates()) {
                    if (eval(argument, predicate)) {
                        yield true;
                    }
                }
                yield false;
            }
            case AND -> {
                for (KeyFilterPredicate predicate : complexPredicate.getPredicates()) {
                    if (!eval(argument, predicate)) {
                        yield false;
                    }
                }
                yield true;
            }
        };
    }

    private boolean evalBooleanPredicate(SingleValueArgumentEntry argument, BooleanFilterPredicate predicate) {
        Boolean value = KvUtil.getBoolValue(argument.getKvEntryValue());
        if (value == null) {
            return false;
        }
        Boolean predicateValue = resolveValue(predicate.getValue(), KvUtil::getBoolValue);
        if (predicateValue == null) {
            return false;
        }
        return switch (predicate.getOperation()) {
            case EQUAL -> value.equals(predicateValue);
            case NOT_EQUAL -> !value.equals(predicateValue);
        };
    }

    private boolean evalNumPredicate(SingleValueArgumentEntry argument, NumericFilterPredicate predicate) {
        Double value = KvUtil.getDoubleValue(argument.getKvEntryValue());
        if (value == null) {
            return false;
        }
        Double predicateValue = resolveValue(predicate.getValue(), KvUtil::getDoubleValue);
        if (predicateValue == null) {
            return false;
        }
        return switch (predicate.getOperation()) {
            case NOT_EQUAL -> !value.equals(predicateValue);
            case EQUAL -> value.equals(predicateValue);
            case GREATER -> value > predicateValue;
            case GREATER_OR_EQUAL -> value >= predicateValue;
            case LESS -> value < predicateValue;
            case LESS_OR_EQUAL -> value <= predicateValue;
        };
    }

    private boolean evalStrPredicate(SingleValueArgumentEntry argument, StringFilterPredicate predicate) {
        String value = KvUtil.getStringValue(argument.getKvEntryValue());
        if (value == null) {
            return false;
        }
        String predicateValue = resolveValue(predicate.getValue(), KvUtil::getStringValue);
        if (predicateValue == null) {
            return false;
        }
        if (predicate.isIgnoreCase()) {
            value = value.toLowerCase();
            predicateValue = predicateValue.toLowerCase();
        }
        return switch (predicate.getOperation()) {
            case CONTAINS -> value.contains(predicateValue);
            case EQUAL -> value.equals(predicateValue);
            case STARTS_WITH -> value.startsWith(predicateValue);
            case ENDS_WITH -> value.endsWith(predicateValue);
            case NOT_EQUAL -> !value.equals(predicateValue);
            case NOT_CONTAINS -> !value.contains(predicateValue);
            case IN -> equalsAny(value, splitByCommaWithoutQuotes(predicateValue));
            case NOT_IN -> !equalsAny(value, splitByCommaWithoutQuotes(predicateValue));
        };
    }

    protected <T> T resolveValue(AlarmConditionValue<T> conditionValue, Function<KvEntry, T> mapper) {
        T value = conditionValue.getStaticValue();
        if (value == null) {
            String argument = conditionValue.getDynamicValueArgument();
            SingleValueArgumentEntry entry = getArgument(argument);
            value = mapper.apply(entry.getKvEntryValue());
            if (value == null) {
                throw new IllegalArgumentException("No value found for argument " + argument);
            }
        }
        return value;
    }

    protected SingleValueArgumentEntry getArgument(String key) {
        SingleValueArgumentEntry entry = (SingleValueArgumentEntry) arguments.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("Argument '" + key + "' is missing");
        }
        return entry;
    }

    private AlarmCalculatedFieldConfiguration getConfiguration(CalculatedFieldCtx ctx) {
        return (AlarmCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
    }

    @Override
    protected void validateNewEntry(ArgumentEntry newEntry) {
        if (!(newEntry instanceof SingleValueArgumentEntry)) {
            throw new IllegalArgumentException("Only single value arguments supported");
        }
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ALARM;
    }

}
