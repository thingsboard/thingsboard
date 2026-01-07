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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  OriginatorSource,
  originatorSourceDescTranslations,
  originatorSourceTranslations
} from '@home/components/rule-node/rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
  selector: 'tb-transformation-node-change-originator-config',
  templateUrl: './change-originator-config.component.html'
})
export class ChangeOriginatorConfigComponent extends RuleNodeConfigurationComponent {

  originatorSource = OriginatorSource;
  originatorSources = Object.keys(OriginatorSource) as OriginatorSource[];
  originatorSourceTranslationMap = originatorSourceTranslations;
  originatorSourceDescTranslationMap = originatorSourceDescTranslations;

  changeOriginatorConfigForm: FormGroup;

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.USER, EntityType.EDGE];

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.changeOriginatorConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.changeOriginatorConfigForm = this.fb.group({
      originatorSource: [configuration ? configuration.originatorSource : null, [Validators.required]],
      entityType: [configuration ? configuration.entityType : null, []],
      entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
      relationsQuery: [configuration ? configuration.relationsQuery : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['originatorSource'];
  }

  protected updateValidators(emitEvent: boolean) {
    const originatorSource: OriginatorSource = this.changeOriginatorConfigForm.get('originatorSource').value;
    if (originatorSource === OriginatorSource.RELATED) {
      this.changeOriginatorConfigForm.get('relationsQuery').setValidators([Validators.required]);
    } else {
      this.changeOriginatorConfigForm.get('relationsQuery').setValidators([]);
    }
    if (originatorSource === OriginatorSource.ENTITY) {
      this.changeOriginatorConfigForm.get('entityType').setValidators([Validators.required]);
      this.changeOriginatorConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.changeOriginatorConfigForm.get('entityType').patchValue(null, {emitEvent});
      this.changeOriginatorConfigForm.get('entityNamePattern').patchValue(null, {emitEvent});
      this.changeOriginatorConfigForm.get('entityType').setValidators([]);
      this.changeOriginatorConfigForm.get('entityNamePattern').setValidators([]);
    }
    this.changeOriginatorConfigForm.get('relationsQuery').updateValueAndValidity({emitEvent});
    this.changeOriginatorConfigForm.get('entityType').updateValueAndValidity({emitEvent});
    this.changeOriginatorConfigForm.get('entityNamePattern').updateValueAndValidity({emitEvent});
  }
}
