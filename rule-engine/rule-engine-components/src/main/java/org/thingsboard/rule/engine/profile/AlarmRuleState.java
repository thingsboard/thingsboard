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

import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

@Data
public class AlarmRuleState {

    private final AlarmSeverity severity;
    private final AlarmRule alarmRule;
    private final long requiredDurationInMs;
    private long lastEventTs;
    private long duration;

    public AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        if (alarmRule.getCondition().getDurationValue() > 0) {
            requiredDurationInMs = alarmRule.getCondition().getDurationUnit().toMillis(alarmRule.getCondition().getDurationValue());
        } else {
            requiredDurationInMs = 0;
        }
    }

    public boolean eval(DeviceDataSnapshot data) {
        if (requiredDurationInMs > 0) {
            boolean eval = eval(alarmRule.getCondition(), data);
            if (eval) {
                if (lastEventTs > 0) {
                    if (data.getTs() > lastEventTs) {
                        duration += data.getTs() - lastEventTs;
                        lastEventTs = data.getTs();
                    }
                } else {
                    lastEventTs = data.getTs();
                    duration = 0;
                }
                return duration > requiredDurationInMs;
            } else {
                lastEventTs = 0;
                duration = 0;
                return false;
            }
        } else {
            return eval(alarmRule.getCondition(), data);
        }
    }

    public boolean eval(long ts) {
        if (requiredDurationInMs > 0 && lastEventTs > 0 && ts > lastEventTs) {
            duration += ts - lastEventTs;
            return duration > requiredDurationInMs;
        } else {
            return false;
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
