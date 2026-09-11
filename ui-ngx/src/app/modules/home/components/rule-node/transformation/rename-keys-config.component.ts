// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { FetchTo, FetchToRenameTranslation } from '../rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';


@Component({
    selector: 'tb-transformation-node-rename-keys-config',
    templateUrl: './rename-keys-config.component.html',
    styleUrls: ['./rename-keys-config.component.scss'],
    standalone: false
})
export class RenameKeysConfigComponent extends RuleNodeConfigurationComponent {
  renameKeysConfigForm: FormGroup;
  renameIn = [];
  translation = FetchToRenameTranslation;

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const key of this.translation.keys()) {
      this.renameIn.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  protected configForm(): FormGroup {
    return this.renameKeysConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.renameKeysConfigForm = this.fb.group({
      renameIn: [configuration ? configuration.renameIn : null, [Validators.required]],
      renameKeysMapping: [configuration ? configuration.renameKeysMapping : null, [Validators.required]]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let renameIn: FetchTo;

    if (isDefinedAndNotNull(configuration?.fromMetadata)) {
      renameIn = configuration.fromMetadata ? FetchTo.METADATA : FetchTo.DATA;
    } else if (isDefinedAndNotNull(configuration?.renameIn)) {
      renameIn = configuration?.renameIn;
    } else {
      renameIn = FetchTo.DATA;
    }

    return {
      renameKeysMapping: isDefinedAndNotNull(configuration?.renameKeysMapping) ? configuration.renameKeysMapping : null,
      renameIn
    };
  }
}
