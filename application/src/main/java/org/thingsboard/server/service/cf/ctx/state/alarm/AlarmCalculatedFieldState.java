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
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
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
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NoDataFilterPredicate;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.thingsboard.server.common.data.StringUtils.equalsAny;
import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;
import static org.thingsboard.server.service.cf.ctx.state.alarm.AlarmEvalResult.Cause.NEW_EVENT;
import static org.thingsboard.server.service.cf.ctx.state.alarm.AlarmEvalResult.Cause.SCHEDULED_REEVALUATION;
import static org.thingsboard.server.service.cf.ctx.state.alarm.AlarmEvalResult.Status.FALSE;
import static org.thingsboard.server.service.cf.ctx.state.alarm.AlarmEvalResult.Status.NOT_YET_TRUE;
import static org.thingsboard.server.service.cf.ctx.state.alarm.AlarmEvalResult.Status.TRUE;

@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AlarmCalculatedFieldState extends BaseCalculatedFieldState {

    private AlarmCalculatedFieldConfiguration configuration;
    private String alarmType;

    @Getter
    private final Map<AlarmSeverity, AlarmRuleState> createRuleStates = new TreeMap<>(Comparator.comparing(Enum::ordinal));
    @Getter
    @Setter
    private AlarmRuleState clearRuleState;

    @Getter
    private Alarm currentAlarm;
    private boolean initialFetchDone;

    // TODO: deprecate device profile node, describe the differences and improvements

    public AlarmCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx) {
        super.setCtx(ctx, actorCtx);
        this.configuration = getConfiguration(ctx);
        this.alarmType = ctx.getCalculatedField().getName();

        Map<AlarmSeverity, AlarmRule> createRules = configuration.getCreateRules();
        createRules.forEach((severity, rule) -> {
            AlarmRuleState ruleState = createRuleStates.get(severity);
            if (ruleState != null) {
                ruleState.setAlarmRule(rule);
            }
        });
        AlarmRule clearRule = configuration.getClearRule();
        if (clearRule != null && clearRuleState != null) {
            clearRuleState.setAlarmRule(clearRule);
        }

        if (currentAlarm != null && !currentAlarm.getType().equals(alarmType)) {
            currentAlarm = null;
            initialFetchDone = false;
        }
    }

    @Override
    public void init(boolean restored) {
        super.init(restored);
        AtomicBoolean reevalNeeded = new AtomicBoolean(false);
        Map<AlarmSeverity, AlarmRule> createRules = configuration.getCreateRules();
        for (AlarmSeverity severity : AlarmSeverity.values()) {
            AlarmRule rule = createRules.get(severity);
            if (rule != null) {
                createRuleStates.compute(severity, (__, ruleState) -> {
                    return initRuleState(severity, rule, ruleState, reevalNeeded);
                });
            } else {
                AlarmRuleState state = createRuleStates.remove(severity);
                if (state != null) {
                    clearState(state);
                }
            }
        }

        AlarmRule clearRule = configuration.getClearRule();
        if (clearRule != null) {
            clearRuleState = initRuleState(null, clearRule, clearRuleState, reevalNeeded);
        } else {
            if (clearRuleState != null) {
                clearState(clearRuleState);
                clearRuleState = null;
            }
        }
        log.debug("Initialized create rule states {} and clear rule state {} for {}", createRuleStates, clearRuleState, configuration);

        if (reevalNeeded.get()) {
            initCurrentAlarm(ctx);
            createOrClearAlarms(state -> {
                if (state.getCondition().getType() == AlarmConditionType.DURATION) {
                    AlarmEvalResult evalResult = state.reeval(System.currentTimeMillis(), ctx);
                    if (evalResult.getStatus() == TRUE || evalResult.getStatus() == NOT_YET_TRUE) {
                        ScheduledFuture<?> future = ctx.scheduleReevaluation(evalResult.getLeftDuration(), actorCtx);
                        if (future != null) {
                            state.setDurationCheckFuture(future);
                        }
                    }
                }
                return AlarmEvalResult.NOT_YET_TRUE;
            }, ctx);
        }
    }

    private AlarmRuleState initRuleState(AlarmSeverity severity, AlarmRule rule, AlarmRuleState ruleState, AtomicBoolean reevalNeeded) {
        if (ruleState == null) {
            ruleState = new AlarmRuleState(severity, rule, this);
        } else {
            // when restored
            ruleState.setAlarmRule(rule);
            ruleState.setActive(null);
            AlarmCondition condition = rule.getCondition();
            if (condition.hasSchedule() || (condition.getType() == AlarmConditionType.DURATION && !ruleState.isEmpty())) {
                reevalNeeded.set(true);
            }
        }
        return ruleState;
    }

    @Override
    public void reset() {
        super.reset();
        configuration = null;
    }

    @Override
    public void close() {
        super.close();
        for (AlarmRuleState state : createRuleStates.values()) {
            clearState(state);
        }
        clearState(clearRuleState);
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        initCurrentAlarm(ctx);
        TbAlarmResult result = createOrClearAlarms(state -> {
            if (updatedArgs != null) {
                boolean newEvent = !updatedArgs.isEmpty();
                AlarmEvalResult evalResult = state.eval(newEvent, ctx);
                if (evalResult.getStatus() == NOT_YET_TRUE && evalResult.getLeftDuration() > 0) {
                    long leftDuration = evalResult.getLeftDuration();
                    ScheduledFuture<?> future = ctx.scheduleReevaluation(leftDuration, actorCtx);
                    if (future != null) {
                        state.setDurationCheckFuture(future);
                    }
                }
                return evalResult.withCause(NEW_EVENT);
            } else {
                return state.reeval(System.currentTimeMillis(), ctx).withCause(SCHEDULED_REEVALUATION);
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
        createRuleStates.values().forEach(this::clearState);
        clearState(clearRuleState);
    }

    private void processAlarmAck(Alarm alarm) {
        currentAlarm.setAcknowledged(alarm.isAcknowledged());
        currentAlarm.setAckTs(alarm.getAckTs());
    }

    private void processAlarmDelete(Alarm alarm) {
        processAlarmClear(alarm);
    }

    private TbAlarmResult createOrClearAlarms(Function<AlarmRuleState, AlarmEvalResult> evalFunction,
                                              CalculatedFieldCtx ctx) {
        AlarmEvalResult evalResult = null;
        AlarmRuleState resultState = null;
        AlarmRuleState.StateInfo resultStateInfo = null;

        for (AlarmRuleState state : createRuleStates.values()) {
            evalResult = evalFunction.apply(state);
            log.debug("Evaluated create rule {} with args {}. Result: {}", state, arguments, evalResult);
            if (evalResult.getStatus() == TRUE) {
                resultState = state;
                break;
            } else if (evalResult.getStatus() == FALSE) {
                clearState(state);
            }
        }

        TbAlarmResult result = null;
        if (resultState != null) {
            result = calculateAlarmResult(resultState, evalResult, ctx);
            resultStateInfo = resultState.getStateInfo();
            log.debug("Alarm result for state {}: {}", resultState, result);
            clearState(clearRuleState);
        } else if (currentAlarm != null && clearRuleState != null) {
            evalResult = evalFunction.apply(clearRuleState);
            log.debug("Evaluated clear rule {} with args {}. Result: {}", clearRuleState, arguments, evalResult);
            if (evalResult.getStatus() == TRUE) {
                resultStateInfo = clearRuleState.getStateInfo();
                clearState(clearRuleState);
                for (AlarmRuleState state : createRuleStates.values()) {
                    clearState(state);
                }
                AlarmApiCallResult clearResult = ctx.getAlarmService().clearAlarm(
                        ctx.getTenantId(), currentAlarm.getId(), System.currentTimeMillis(), createDetails(clearRuleState), false
                );
                if (clearResult.isCleared()) {
                    result = TbAlarmResult.builder()
                            .isCleared(true)
                            .alarm(clearResult.getAlarm())
                            .build();
                    resultState = clearRuleState;
                }
                currentAlarm = null;
            } else if (evalResult.getStatus() == FALSE) {
                clearState(clearRuleState);
            }
        }
        if (result != null && resultState != null) {
            result.setConditionRepeats(resultStateInfo.eventCount());
            result.setConditionDuration(resultStateInfo.duration());
        }
        return result;
    }

    private void clearState(AlarmRuleState state) {
        if (state != null) {
            log.debug("Clearing rule state {}", state);
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

    private TbAlarmResult calculateAlarmResult(AlarmRuleState ruleState, AlarmEvalResult evalResult, CalculatedFieldCtx ctx) {
        AlarmSeverity severity = ruleState.getSeverity();
        if (currentAlarm != null) {
            AlarmSeverity oldSeverity = currentAlarm.getSeverity();
            if (severity.ordinal() > oldSeverity.ordinal()) {
                log.trace("Skipping alarm update for result state {} for eval result {} because severity is decreased", ruleState, evalResult);
                return null;
            }
            if (severity.ordinal() == oldSeverity.ordinal() && evalResult.getCause() == SCHEDULED_REEVALUATION) {
                log.trace("Skipping alarm update for result state {} for eval result {}", ruleState, evalResult);
                return null;
            }

            currentAlarm.setEndTs(System.currentTimeMillis());
            currentAlarm.setDetails(createDetails(ruleState));
            currentAlarm.setSeverity(severity);
            AlarmApiCallResult result = ctx.getAlarmService().updateAlarm(AlarmUpdateRequest.fromAlarm(currentAlarm));
            currentAlarm = result.getAlarm();
            return TbAlarmResult.fromAlarmResult(result);
        } else {
            var newAlarm = new Alarm();
            newAlarm.setType(alarmType);
            newAlarm.setAcknowledged(false);
            newAlarm.setCleared(false);
            newAlarm.setSeverity(severity);
            long startTs = latestTimestamp;
            long currentTime = System.currentTimeMillis();
            if (startTs <= 0L || startTs > currentTime) {
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
                case AND -> simpleExpression.getFilters().stream()
                        .allMatch(filter -> eval(getArgument(filter.getArgument()), filter));
                case OR -> simpleExpression.getFilters().stream()
                        .anyMatch(filter -> eval(getArgument(filter.getArgument()), filter));
            };
        }
    }

    private boolean eval(SingleValueArgumentEntry argument, AlarmConditionFilter filter) {
        ComplexOperation operation = filter.getOperation();
        if (operation == null) {
            operation = ComplexOperation.AND;
        }
        return switch (operation) {
            case AND -> filter.getPredicates().stream()
                    .allMatch(predicate -> eval(argument, predicate));
            case OR -> filter.getPredicates().stream()
                    .anyMatch(predicate -> eval(argument, predicate));
        };
    }

    private boolean eval(SingleValueArgumentEntry argument, KeyFilterPredicate predicate) {
        return switch (predicate.getType()) {
            case STRING -> evalStrPredicate(argument, (StringFilterPredicate) predicate);
            case NUMERIC -> evalNumPredicate(argument, (NumericFilterPredicate) predicate);
            case BOOLEAN -> evalBooleanPredicate(argument, (BooleanFilterPredicate) predicate);
            case NO_DATA -> evalNoDataPredicate(argument, (NoDataFilterPredicate) predicate);
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

    private boolean evalNoDataPredicate(SingleValueArgumentEntry argument, NoDataFilterPredicate predicate) {
        long passedMs = System.currentTimeMillis() - argument.getTs();
        long duration = resolveValue(predicate.getDuration(), KvUtil::getLongValue);
        if (duration > 0) {
            long requiredDuration = predicate.getUnit().toMillis(duration);
            log.trace("[{}] No data for argument {} during {} ms, required duration: {} ms", ctx, argument, passedMs, requiredDuration);
            return passedMs >= requiredDuration;
        } else {
            return false;
        }
    }

    protected <T> T resolveValue(AlarmConditionValue<T> conditionValue, Function<KvEntry, T> mapper) {
        T value = conditionValue.getStaticValue();
        if (value == null) {
            String argument = conditionValue.getDynamicValueArgument();
            SingleValueArgumentEntry entry = getArgument(argument);
            value = mapper.apply(entry.getKvEntryValue());
            if (value == null) {
                throw new IllegalArgumentException("No proper value found for argument " + argument);
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
    protected void validateNewEntry(String key, ArgumentEntry newEntry) {
        if (!(newEntry instanceof SingleValueArgumentEntry)) {
            throw new IllegalArgumentException("Only single value arguments supported");
        }
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ALARM;
    }

}
