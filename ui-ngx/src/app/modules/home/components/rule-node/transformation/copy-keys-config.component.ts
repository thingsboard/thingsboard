// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { isDefinedAndNotNull } from '@core/public-api';
import { TranslateService } from '@ngx-translate/core';
import { FetchFromToTranslation, FetchTo } from '../rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-transformation-node-copy-keys-config',
    templateUrl: './copy-keys-config.component.html',
    styleUrls: [],
    standalone: false
})

export class CopyKeysConfigComponent extends RuleNodeConfigurationComponent{
  copyKeysConfigForm: FormGroup;
  copyFrom = [];
  translation = FetchFromToTranslation;

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const key of this.translation.keys()) {
      this.copyFrom.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.copyKeysConfigForm = this.fb.group({
      copyFrom: [configuration.copyFrom , [Validators.required]],
      keys: [configuration ? configuration.keys : null, [Validators.required]]
    });
  }

  protected configForm(): FormGroup {
    return this.copyKeysConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let copyFrom: FetchTo;

    if (isDefinedAndNotNull(configuration?.fromMetadata)) {
      copyFrom = configuration.copyFrom ? FetchTo.METADATA : FetchTo.DATA;
    } else if (isDefinedAndNotNull(configuration?.copyFrom)) {
      copyFrom = configuration.copyFrom;
    } else {
      copyFrom = FetchTo.DATA;
    }

    return {
      keys: isDefinedAndNotNull(configuration?.keys) ? configuration.keys : null,
      copyFrom
    };
  }
}
