// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { deepClone } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  AggIntervalType,
  aggIntervalTypeTranslations,
  AggMathFunction,
  aggMathFunctionTranslations,
  defaultRelationsQuery,
  IntervalPersistPolicy,
  intervalPersistPolicyTranslations,
  ParentEntitiesQueryType,
  prepareParentEntitiesQuery,
  StatePersistPolicy,
  statePersistPolicyTranslations,
  TimeUnit,
  timeUnitTranslations
} from '../rule-node-config.models';

const intervalValidators = [Validators.required,
  Validators.min(1), Validators.max(2147483647)];

@Component({
    selector: 'tb-analytics-node-aggregate-incoming-config',
    templateUrl: './aggregate-incoming-config.component.html',
    styleUrls: ['./aggregate-incoming-config.component.scss'],
    standalone: false
})
export class AggregateIncomingConfigComponent extends RuleNodeConfigurationComponent {

  aggregateIncomingConfigForm: UntypedFormGroup;

  aggPeriodTimeUnits: TimeUnit[] = [TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS];
  timeUnitsTranslationMap = timeUnitTranslations;

  mathFunctions = Object.values(AggMathFunction);
  mathFunctionsTranslationMap = aggMathFunctionTranslations;

  aggIntervalType = AggIntervalType;
  aggIntervalTypes = Object.values(AggIntervalType);
  aggIntervalTypesTranslationMap = aggIntervalTypeTranslations;

  intervalPersistPolicies = Object.values(IntervalPersistPolicy);
  intervalPersistPolicyTranslationMap = intervalPersistPolicyTranslations;

  statePersistPolicies = Object.values(StatePersistPolicy);
  statePersistPolicyTranslationMap = statePersistPolicyTranslations;
  StatePersistPolicy = StatePersistPolicy;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.aggregateIncomingConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.aggregateIncomingConfigForm = this.fb.group({
      inputValueKey: [configuration ? configuration.inputValueKey : null, [Validators.required]],
      outputValueKey: [configuration ? configuration.outputValueKey : null, [Validators.required]],
      mathFunction: [{value : configuration ? configuration.mathFunction : null, disabled: this.ruleNodeId}, [Validators.required]],
      aggIntervalType: [{value : configuration && configuration.aggIntervalType ?
          configuration.aggIntervalType : AggIntervalType.CUSTOM, disabled: this.ruleNodeId}, [Validators.required]],
      timeZoneId: [{value : configuration ? configuration.timeZoneId : null, disabled: this.ruleNodeId}, []],
      aggIntervalValue: [configuration ? configuration.aggIntervalValue : null, []],
      aggIntervalTimeUnit: [configuration ? configuration.aggIntervalValue : null, []],
      intervalPersistencePolicy: [configuration ? configuration.intervalPersistencePolicy : null, [Validators.required]],
      outMsgType: [configuration ? configuration.outMsgType : null, [Validators.required]],
      intervalCheckValue: [configuration ? configuration.intervalCheckValue : null, intervalValidators],
      intervalCheckTimeUnit: [configuration ? configuration.intervalCheckTimeUnit : null, [Validators.required]],
      statePersistencePolicy: [configuration ? configuration.statePersistencePolicy : null, [Validators.required]],
      statePersistenceValue: [configuration ? configuration.statePersistenceValue : null, intervalValidators],
      statePersistenceTimeUnit: [configuration ? configuration.statePersistenceTimeUnit : null, [Validators.required]],
      autoCreateIntervals: [configuration ? configuration.autoCreateIntervals : false, []],
      periodValue: [configuration ? configuration.periodValue : null, []],
      periodTimeUnit: [configuration ? configuration.periodTimeUnit : null, []],
      parentEntitiesQuery: this.fb.group(
        {
          type: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.type : null, [Validators.required]],
          rootEntityId: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.rootEntityId : null, []],
          relationsQuery: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.relationsQuery : null, []],
          entityId: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.entityId : null, []],
          entityGroupId: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.entityGroupId : null, []]
        }
      )
    });
  }

  protected validatorTriggers(): string[] {
    return ['aggIntervalType', 'parentEntitiesQuery.type', 'autoCreateIntervals'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const parentEntitiesQueryControl = this.aggregateIncomingConfigForm.get('parentEntitiesQuery');
    const parentEntitiesQueryType: ParentEntitiesQueryType = parentEntitiesQueryControl.get('type').value;
    if (emitEvent) {
      if (trigger === 'parentEntitiesQuery.type') {
        const parentEntitiesQuery = {
          type: parentEntitiesQueryType
        } as any;
        if (parentEntitiesQueryType === 'relationsQuery') {
          parentEntitiesQuery.rootEntityId = null;
          parentEntitiesQuery.relationsQuery = deepClone(defaultRelationsQuery);
        } else if (parentEntitiesQueryType === 'single') {
          parentEntitiesQuery.entityId = null;
        } else if (parentEntitiesQueryType === 'group') {
          parentEntitiesQuery.entityGroupId = null;
        }
        parentEntitiesQueryControl.reset(parentEntitiesQuery, {emitEvent: false});
      }
    }
    const aggIntervalType: AggIntervalType = this.aggregateIncomingConfigForm.get('aggIntervalType').value;
    if (aggIntervalType === AggIntervalType.CUSTOM) {
      this.aggregateIncomingConfigForm.get('timeZoneId').setValidators([]);
      this.aggregateIncomingConfigForm.get('aggIntervalValue').setValidators(intervalValidators);
      this.aggregateIncomingConfigForm.get('aggIntervalTimeUnit').setValidators([Validators.required]);
    } else {
      this.aggregateIncomingConfigForm.get('timeZoneId').setValidators([Validators.required]);
      this.aggregateIncomingConfigForm.get('aggIntervalValue').setValidators([]);
      this.aggregateIncomingConfigForm.get('aggIntervalTimeUnit').setValidators([]);
    }
    this.aggregateIncomingConfigForm.get('timeZoneId').updateValueAndValidity({emitEvent});
    this.aggregateIncomingConfigForm.get('aggIntervalValue').updateValueAndValidity({emitEvent});
    this.aggregateIncomingConfigForm.get('aggIntervalTimeUnit').updateValueAndValidity({emitEvent});

    const autoCreateIntervals: boolean = this.aggregateIncomingConfigForm.get('autoCreateIntervals').value;
    parentEntitiesQueryControl.get('rootEntityId').setValidators([]);
    parentEntitiesQueryControl.get('relationsQuery').setValidators([]);
    parentEntitiesQueryControl.get('entityId').setValidators([]);
    parentEntitiesQueryControl.get('entityGroupId').setValidators([]);

    if (autoCreateIntervals) {
      this.aggregateIncomingConfigForm.get('periodValue').setValidators(intervalValidators);
      this.aggregateIncomingConfigForm.get('periodTimeUnit').setValidators([Validators.required]);
      if (parentEntitiesQueryType === 'relationsQuery') {
        parentEntitiesQueryControl.get('rootEntityId').setValidators([Validators.required]);
        parentEntitiesQueryControl.get('relationsQuery').setValidators([Validators.required]);
      } else if (parentEntitiesQueryType === 'single') {
        parentEntitiesQueryControl.get('entityId').setValidators([Validators.required]);
      } else if (parentEntitiesQueryType === 'group') {
        parentEntitiesQueryControl.get('entityGroupId').setValidators([Validators.required]);
      }
    } else {
      this.aggregateIncomingConfigForm.get('periodValue').setValidators([]);
      this.aggregateIncomingConfigForm.get('periodTimeUnit').setValidators([]);
    }
    this.aggregateIncomingConfigForm.get('periodValue').updateValueAndValidity({emitEvent});
    this.aggregateIncomingConfigForm.get('periodTimeUnit').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('rootEntityId').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('relationsQuery').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('entityId').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('entityGroupId').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(): RuleNodeConfiguration {
    const configuration = this.configForm().getRawValue();
    configuration.parentEntitiesQuery = prepareParentEntitiesQuery(configuration.parentEntitiesQuery);
    return configuration;
  }
}
