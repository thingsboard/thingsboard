///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntitySearchDirection, entitySearchDirectionTranslations } from '@app/shared/models/relation.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
  selector: 'tb-filter-node-check-relation-config',
  templateUrl: './check-relation-config.component.html',
  styleUrls: ['./check-relation-config.component.scss']
})
export class CheckRelationConfigComponent extends RuleNodeConfigurationComponent {

  checkRelationConfigForm: UntypedFormGroup;

  entitySearchDirection: Array<EntitySearchDirection> = Object.values(EntitySearchDirection);
  entitySearchDirectionTranslationsMap = entitySearchDirectionTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.checkRelationConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      checkForSingleEntity: isDefinedAndNotNull(configuration?.checkForSingleEntity) ? configuration.checkForSingleEntity : false,
      direction: isDefinedAndNotNull(configuration?.direction) ? configuration.direction : null,
      entityType: isDefinedAndNotNull(configuration?.entityType) ? configuration.entityType : null,
      entityId: isDefinedAndNotNull(configuration?.entityId) ? configuration.entityId : null,
      relationType: isDefinedAndNotNull(configuration?.relationType) ? configuration.relationType : null
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.checkRelationConfigForm = this.fb.group({
      checkForSingleEntity: [configuration.checkForSingleEntity, []],
      direction: [configuration.direction, []],
      entityType: [configuration.entityType,
        configuration && configuration.checkForSingleEntity ? [Validators.required] : []],
      entityId: [configuration.entityId,
        configuration && configuration.checkForSingleEntity ? [Validators.required] : []],
      relationType: [configuration.relationType, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['checkForSingleEntity'];
  }

  protected updateValidators(emitEvent: boolean) {
    const checkForSingleEntity: boolean = this.checkRelationConfigForm.get('checkForSingleEntity').value;
    this.checkRelationConfigForm.get('entityType').setValidators(checkForSingleEntity ? [Validators.required] : []);
    this.checkRelationConfigForm.get('entityType').updateValueAndValidity({emitEvent});
    this.checkRelationConfigForm.get('entityId').setValidators(checkForSingleEntity ? [Validators.required] : []);
    this.checkRelationConfigForm.get('entityId').updateValueAndValidity({emitEvent});
  }

}
