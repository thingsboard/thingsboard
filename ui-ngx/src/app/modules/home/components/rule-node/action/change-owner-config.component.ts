// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { OwnerType, ownerTypeTranslations } from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-action-node-change-owner-config',
    templateUrl: './change-owner-config.component.html',
    styleUrls: [],
    standalone: false
})
export class ChangeOwnerConfigComponent extends RuleNodeConfigurationComponent {

  changeOwnerConfigForm: UntypedFormGroup;

  ownerType = OwnerType;
  ownerTypes = Object.values(OwnerType);
  ownerTypeTranslationsMap = ownerTypeTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.changeOwnerConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      ownerType: isDefinedAndNotNull(configuration?.ownerType) ? configuration.ownerType : OwnerType.TENANT,
      ownerNamePattern: isDefinedAndNotNull(configuration?.ownerNamePattern) ? configuration.ownerNamePattern : null,
      createOwnerIfNotExists: isDefinedAndNotNull(configuration?.createOwnerIfNotExists) ? configuration.createOwnerIfNotExists : false,
      createOwnerOnOriginatorLevel: isDefinedAndNotNull(configuration?.createOwnerOnOriginatorLevel) ? configuration.createOwnerOnOriginatorLevel : false
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.changeOwnerConfigForm = this.fb.group({
      ownerType: [configuration ? configuration.ownerType : null, [Validators.required]],
      ownerNamePattern: [configuration ? configuration.ownerNamePattern : null, []],
      createOwnerIfNotExists: [configuration ? configuration.createOwnerIfNotExists : false, []],
      createOwnerOnOriginatorLevel: [configuration ? configuration.createOwnerOnOriginatorLevel : false, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['ownerType', 'createOwnerIfNotExists'];
  }

  protected updateValidators(emitEvent: boolean) {
    const ownerType: OwnerType = this.changeOwnerConfigForm.get('ownerType').value;
    const createOwnerIfNotExists: boolean = this.changeOwnerConfigForm.get('createOwnerIfNotExists').value;
    if (ownerType === OwnerType.CUSTOMER) {
      this.changeOwnerConfigForm.get('ownerNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.changeOwnerConfigForm.get('ownerNamePattern').setValidators([]);
    }
    if (createOwnerIfNotExists) {
      this.changeOwnerConfigForm.get('createOwnerOnOriginatorLevel').enable({emitEvent});
    } else {
      this.changeOwnerConfigForm.get('createOwnerOnOriginatorLevel').disable({emitEvent});
    }
    this.changeOwnerConfigForm.get('ownerNamePattern').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.ownerNamePattern = configuration.ownerNamePattern ? configuration.ownerNamePattern.trim() : null;
    return configuration;
  }
}
