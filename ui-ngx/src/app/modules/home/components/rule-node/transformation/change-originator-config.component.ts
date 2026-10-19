// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    templateUrl: './change-originator-config.component.html',
    standalone: false
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
