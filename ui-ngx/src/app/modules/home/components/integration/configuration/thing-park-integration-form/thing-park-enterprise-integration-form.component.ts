// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
import { UntypedFormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  ThingParkIntegrationFormComponent
} from '@home/components/integration/configuration/thing-park-integration-form/thing-park-integration-form.component';

@Component({
    selector: 'tb-thing-spark-enterprise-integration-form',
    templateUrl: './thing-park-integration-form.component.html',
    styleUrls: ['./thing-park-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ThingParkEnterpriseIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ThingParkEnterpriseIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class ThingParkEnterpriseIntegrationFormComponent extends ThingParkIntegrationFormComponent {

  protected integrationType = IntegrationType.TPE;

  constructor(protected fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              protected translate: TranslateService) {
    super(fb, store, translate);
  }

  ngOnInit() {
    super.ngOnInit();
    this.thingParkConfigForm.get('enableSecurity').disable({emitEvent: true});
    this.thingParkConfigForm.get('enableSecurity').setValue(true);
  }

  setDisabledState(isDisabled: boolean) {
    super.setDisabledState(isDisabled);
    this.thingParkConfigForm.get('enableSecurity').disable({emitEvent: false});
  }
}
