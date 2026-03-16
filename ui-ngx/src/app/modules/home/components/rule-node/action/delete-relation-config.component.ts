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
import { EntityType } from '@app/shared/models/entity-type.models';
import { EntitySearchDirection } from '@app/shared/models/relation.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-delete-relation-config',
    templateUrl: './delete-relation-config.component.html',
    styleUrls: [],
    standalone: false
})
export class DeleteRelationConfigComponent extends RuleNodeConfigurationComponent {

  directionTypes = Object.keys(EntitySearchDirection);

  directionTypeTranslations  = new Map<EntitySearchDirection, string>(
    [
      [EntitySearchDirection.FROM, 'rule-node-config.del-relation-direction-from'],
      [EntitySearchDirection.TO, 'rule-node-config.del-relation-direction-to'],
    ]
  );

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

  entityType = EntityType;

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.TENANT,
    EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.EDGE];

  deleteRelationConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.deleteRelationConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deleteRelationConfigForm = this.fb.group({
      deleteForSingleEntity: [configuration ? configuration.deleteForSingleEntity : false, []],
      direction: [configuration ? configuration.direction : null, [Validators.required]],
      entityType: [configuration ? configuration.entityType : null, []],
      entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
      relationType: [configuration ? configuration.relationType : null, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['deleteForSingleEntity', 'entityType'];
  }

  protected updateValidators(emitEvent: boolean) {
    const deleteForSingleEntity: boolean = this.deleteRelationConfigForm.get('deleteForSingleEntity').value;
    const entityType: EntityType = this.deleteRelationConfigForm.get('entityType').value;
    if (deleteForSingleEntity) {
      this.deleteRelationConfigForm.get('entityType').setValidators([Validators.required]);
    } else {
      this.deleteRelationConfigForm.get('entityType').setValidators([]);
    }
    if (deleteForSingleEntity && entityType && entityType !== EntityType.TENANT) {
      this.deleteRelationConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.deleteRelationConfigForm.get('entityNamePattern').setValidators([]);
    }
    this.deleteRelationConfigForm.get('entityType').updateValueAndValidity({emitEvent: false});
    this.deleteRelationConfigForm.get('entityNamePattern').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.entityNamePattern = configuration.entityNamePattern ? configuration.entityNamePattern.trim() : null;
    return configuration;
  }
}
