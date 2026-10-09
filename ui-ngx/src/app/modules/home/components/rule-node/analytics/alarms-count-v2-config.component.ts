// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { TimeUnit, timeUnitTranslations } from '../rule-node-config.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
    selector: 'tb-analytics-node-alarms-count-v2-config',
    templateUrl: './alarms-count-v2-config.component.html',
    styleUrls: ['./alarms-count-v2-config.component.scss'],
    standalone: false
})
export class AlarmsCountV2ConfigComponent extends RuleNodeConfigurationComponent {

  alarmsCountConfigForm: UntypedFormGroup;

  timeUnits = Object.keys(TimeUnit);
  timeUnitsTranslationMap = timeUnitTranslations;

  propagationEntityTypes: EntityType[] = [
    EntityType.DEVICE,
    EntityType.ASSET,
    EntityType.ENTITY_VIEW,
    EntityType.TENANT,
    EntityType.CUSTOMER,
    EntityType.USER,
    EntityType.DASHBOARD,
    EntityType.RULE_CHAIN,
    EntityType.RULE_NODE,
    EntityType.ENTITY_GROUP,
    EntityType.CONVERTER,
    EntityType.INTEGRATION,
    EntityType.SCHEDULER_EVENT,
    EntityType.BLOB_ENTITY,
    EntityType.REPORT_TEMPLATE,
    EntityType.REPORT
  ];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.alarmsCountConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.alarmsCountConfigForm = this.fb.group({
      alarmsCountMappings: [configuration ? configuration.alarmsCountMappings : null, [Validators.required]],
      countAlarmsForPropagationEntities: [configuration ? configuration.alarmsCountMappings : true, [Validators.required]],
      propagationEntityTypes: [configuration && configuration.propagationEntityTypes ? configuration.propagationEntityTypes : [], []],
      outMsgType: [configuration ? configuration.outMsgType : null, [Validators.required]]
    });
  }
}
