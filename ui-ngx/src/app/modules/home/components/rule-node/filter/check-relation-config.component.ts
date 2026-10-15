// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntitySearchDirection, entitySearchDirectionTranslations } from '@app/shared/models/relation.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-filter-node-check-relation-config',
    templateUrl: './check-relation-config.component.html',
    styleUrls: ['./check-relation-config.component.scss'],
    standalone: false
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
      entityType: [configuration.entityType, [Validators.required]],
      entityId: [configuration.entityId, [Validators.required]],
      relationType: [configuration.relationType, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['checkForSingleEntity', 'entityType'];
  }

  protected updateValidators(_emitEvent: boolean, trigger: string) {
    if (trigger === 'entityType') {
      this.checkRelationConfigForm.get('entityId').reset(null);
    } else {
      const checkForSingleEntity: boolean = this.checkRelationConfigForm.get('checkForSingleEntity').value;
      if (checkForSingleEntity) {
        this.checkRelationConfigForm.get('entityType').enable({emitEvent: false});
        this.checkRelationConfigForm.get('entityId').enable({emitEvent: false});
      } else {
        this.checkRelationConfigForm.get('entityType').disable({emitEvent: false});
        this.checkRelationConfigForm.get('entityId').disable({emitEvent: false});
      }
    }
  }

}
