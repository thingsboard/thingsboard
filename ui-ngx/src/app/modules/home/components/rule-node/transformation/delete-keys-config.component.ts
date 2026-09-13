// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { isDefinedAndNotNull } from '@core/public-api';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { FetchTo, FetchToTranslation } from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-transformation-node-delete-keys-config',
    templateUrl: './delete-keys-config.component.html',
    styleUrls: [],
    standalone: false
})

export class DeleteKeysConfigComponent extends RuleNodeConfigurationComponent {

  deleteKeysConfigForm: FormGroup;
  deleteFrom = [];
  translation = FetchToTranslation;

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const key of this.translation.keys()) {
      this.deleteFrom.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deleteKeysConfigForm = this.fb.group({
      deleteFrom: [configuration.deleteFrom, [Validators.required]],
      keys: [configuration ? configuration.keys : null, [Validators.required]]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let deleteFrom: FetchTo;

    if (isDefinedAndNotNull(configuration?.fromMetadata)) {
      deleteFrom = configuration.fromMetadata ? FetchTo.METADATA : FetchTo.DATA;
    } else if (isDefinedAndNotNull(configuration?.deleteFrom)) {
      deleteFrom = configuration?.deleteFrom;
    } else {
      deleteFrom = FetchTo.DATA;
    }

    return {
      keys: isDefinedAndNotNull(configuration?.keys) ? configuration.keys : null,
      deleteFrom
    };
  }

  protected configForm(): FormGroup {
    return this.deleteKeysConfigForm;
  }
}
