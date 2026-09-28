// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { deepClone } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  defaultRelationsQuery,
  ParentEntitiesQueryType,
  prepareParentEntitiesQuery,
  TimeUnit,
  timeUnitTranslations
} from '../rule-node-config.models';

const intervalValidators = [Validators.required,
  Validators.min(1), Validators.max(2147483647)];

@Component({
    selector: 'tb-analytics-node-aggregate-latest-config',
    templateUrl: './aggregate-latest-config.component.html',
    styleUrls: ['./aggregate-latest-config.component.scss'],
    standalone: false
})
export class AggregateLatestConfigComponent extends RuleNodeConfigurationComponent {

  aggregateLatestConfigForm: UntypedFormGroup;

  aggPeriodTimeUnits: TimeUnit[] = [TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS];
  timeUnitsTranslationMap = timeUnitTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.aggregateLatestConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.aggregateLatestConfigForm = this.fb.group({
      periodValue: [configuration ? configuration.periodValue : null, intervalValidators],
      periodTimeUnit: [configuration ? configuration.periodTimeUnit : null, [Validators.required]],
      outMsgType: [configuration ? configuration.outMsgType : null, [Validators.required]],
      parentEntitiesQuery: this.fb.group(
        {
          type: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.type : null, [Validators.required]],
          rootEntityId: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.rootEntityId : null, []],
          relationsQuery: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.relationsQuery : null, []],
          includeRootEntity: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.includeRootEntity : null, []],
          entityId: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.entityId : null, []],
          entityGroupId: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.entityGroupId : null, []],
          childRelationsQuery: [configuration && configuration.parentEntitiesQuery ?
            configuration.parentEntitiesQuery.childRelationsQuery : null, []]
        }
      ),
      aggMappings: [configuration ? configuration.aggMappings : null, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['parentEntitiesQuery.type'];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const parentEntitiesQueryControl = this.aggregateLatestConfigForm.get('parentEntitiesQuery');
    const parentEntitiesQueryType: ParentEntitiesQueryType = parentEntitiesQueryControl.get('type').value;
    if (emitEvent) {
      if (trigger === 'parentEntitiesQuery.type') {
        const parentEntitiesQuery = {
          type: parentEntitiesQueryType
        } as any;
        if (parentEntitiesQueryType === 'relationsQuery') {
          parentEntitiesQuery.rootEntityId = null;
          parentEntitiesQuery.relationsQuery = deepClone(defaultRelationsQuery);
          parentEntitiesQuery.childRelationsQuery = deepClone(defaultRelationsQuery);
        } else if (parentEntitiesQueryType === 'single') {
          parentEntitiesQuery.entityId = null;
          parentEntitiesQuery.childRelationsQuery = deepClone(defaultRelationsQuery);
        } else if (parentEntitiesQueryType === 'group') {
          parentEntitiesQuery.entityGroupId = null;
        }
        parentEntitiesQueryControl.reset(parentEntitiesQuery, {emitEvent: false});
      }
    }

    parentEntitiesQueryControl.get('rootEntityId').setValidators([]);
    parentEntitiesQueryControl.get('relationsQuery').setValidators([]);
    parentEntitiesQueryControl.get('entityId').setValidators([]);
    parentEntitiesQueryControl.get('entityGroupId').setValidators([]);
    parentEntitiesQueryControl.get('childRelationsQuery').setValidators([]);

    if (parentEntitiesQueryType === 'relationsQuery') {
      parentEntitiesQueryControl.get('rootEntityId').setValidators([Validators.required]);
      parentEntitiesQueryControl.get('relationsQuery').setValidators([Validators.required]);
      parentEntitiesQueryControl.get('childRelationsQuery').setValidators([Validators.required]);
    } else if (parentEntitiesQueryType === 'single') {
      parentEntitiesQueryControl.get('entityId').setValidators([Validators.required]);
      parentEntitiesQueryControl.get('childRelationsQuery').setValidators([Validators.required]);
    } else if (parentEntitiesQueryType === 'group') {
      parentEntitiesQueryControl.get('entityGroupId').setValidators([Validators.required]);
    }
    parentEntitiesQueryControl.get('rootEntityId').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('relationsQuery').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('entityId').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('entityGroupId').updateValueAndValidity({emitEvent});
    parentEntitiesQueryControl.get('childRelationsQuery').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.parentEntitiesQuery = prepareParentEntitiesQuery(configuration.parentEntitiesQuery);
    return configuration;
  }
}
