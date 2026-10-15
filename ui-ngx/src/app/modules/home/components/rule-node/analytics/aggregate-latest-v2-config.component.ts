// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { EntitySearchDirection, entitySearchDirectionTranslations } from '@shared/models/relation.models';

@Component({
    selector: 'tb-analytics-node-aggregate-latest-v2-config',
    templateUrl: './aggregate-latest-v2-config.component.html',
    styleUrls: ['./aggregate-latest-v2-config.component.scss'],
    standalone: false
})
export class AggregateLatestV2ConfigComponent extends RuleNodeConfigurationComponent {

  aggregateLatestV2ConfigForm: UntypedFormGroup;

  directionTypes = Object.values(EntitySearchDirection);
  directionTypeTranslations = entitySearchDirectionTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.aggregateLatestV2ConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.aggregateLatestV2ConfigForm = this.fb.group({
      direction: [configuration ? configuration.direction : null, [Validators.required]],
      relationType: [configuration ? configuration.relationType : null, [Validators.required]],
      deduplicationInSec: [configuration ? configuration.deduplicationInSec : null, [Validators.required,
        Validators.min(10), Validators.max(2147483647)]],
      aggMappings: [configuration ? configuration.aggMappings : null, [Validators.required]],
      outMsgType: [configuration ? configuration.outMsgType : null, [Validators.required]]
    });
  }
}
