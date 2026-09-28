// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
import { FormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TtnIntegrationFormComponent } from './ttn-integration-form.component';

@Component({
    selector: 'tb-tti-integration-form',
    templateUrl: './tts-integration-form.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TtiIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TtiIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class TtiIntegrationFormComponent extends TtnIntegrationFormComponent {

  hostRegionSuffix = '.cloud.thethings.industries';

  hideSelectVersion = true;

  constructor(protected fb: FormBuilder) {
    super(fb);
    this.ttnIntegrationConfigForm.get('topicFilters').enable({emitEvent: false});
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ttnIntegrationConfigForm.disable({emitEvent: false});
      this.hostEdit.disable({emitEvent: false});
    } else {
      this.ttnIntegrationConfigForm.enable({emitEvent: false});
      this.hostEdit.enable({emitEvent: false});
    }
  }
}
