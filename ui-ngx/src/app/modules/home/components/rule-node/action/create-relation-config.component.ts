///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EntitySearchDirection } from '@app/shared/models/relation.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
  selector: 'tb-action-node-create-relation-config',
  templateUrl: './create-relation-config.component.html',
  styleUrls: []
})
export class CreateRelationConfigComponent extends RuleNodeConfigurationComponent {

  directionTypes = Object.keys(EntitySearchDirection);
  directionTypeTranslations  = new Map<EntitySearchDirection, string>(
    [
      [EntitySearchDirection.FROM, 'rule-node-config.search-direction-from'],
      [EntitySearchDirection.TO, 'rule-node-config.search-direction-to'],
    ]
  );

  entityType = EntityType;

  entityTypeNamePatternTranslation = new Map<EntityType, string>(
    [
      [EntityType.DEVICE, 'rule-node-config.device-name-pattern'],
      [EntityType.ASSET, 'rule-node-config.asset-name-pattern'],
      [EntityType.ENTITY_VIEW, 'rule-node-config.entity-view-name-pattern'],
      [EntityType.CUSTOMER, 'rule-node-config.customer-title-pattern'],
      [EntityType.USER, 'rule-node-config.user-name-pattern'],
      [EntityType.DASHBOARD, 'rule-node-config.dashboard-name-pattern'],
      [EntityType.EDGE, 'rule-node-config.edge-name-pattern']
    ]
  );

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.TENANT,
    EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.EDGE];

  createRelationConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.createRelationConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.createRelationConfigForm = this.fb.group({
      direction: [configuration ? configuration.direction : null, [Validators.required]],
      entityType: [configuration ? configuration.entityType : null, [Validators.required]],
      entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
      entityTypePattern: [configuration ? configuration.entityTypePattern : null, []],
      relationType: [configuration ? configuration.relationType : null, [Validators.required]],
      createEntityIfNotExists: [configuration ? configuration.createEntityIfNotExists : false, []],
      removeCurrentRelations: [configuration ? configuration.removeCurrentRelations : false, []],
      changeOriginatorToRelatedEntity: [configuration ? configuration.changeOriginatorToRelatedEntity : false, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['entityType', 'createEntityIfNotExists'];
  }

  protected updateValidators(emitEvent: boolean) {
    const entityType: EntityType = this.createRelationConfigForm.get('entityType').value;
    if (entityType && entityType !== EntityType.TENANT) {
      this.createRelationConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.createRelationConfigForm.get('entityNamePattern').setValidators([]);
    }
    if (entityType && (entityType === EntityType.DEVICE || entityType === EntityType.ASSET)) {
      const validators = [Validators.pattern(/.*\S.*/)]
      if (this.createRelationConfigForm.get('createEntityIfNotExists').value) {
        validators.push(Validators.required);
      }
      this.createRelationConfigForm.get('entityTypePattern').setValidators(validators);
    } else {
      this.createRelationConfigForm.get('entityTypePattern').setValidators([]);
    }
    this.createRelationConfigForm.get('entityNamePattern').updateValueAndValidity({emitEvent});
    this.createRelationConfigForm.get('entityTypePattern').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.entityNamePattern = configuration.entityNamePattern ? configuration.entityNamePattern.trim() : null;
    configuration.entityTypePattern = configuration.entityTypePattern ? configuration.entityTypePattern.trim() : null;
    return configuration;
  }
}
