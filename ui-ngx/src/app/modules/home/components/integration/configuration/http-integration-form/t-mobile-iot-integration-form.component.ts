// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
import { UntypedFormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { HttpIntegrationFormComponent } from './http-integration-form.component';

@Component({
    selector: 'tb-t-mobile-iot-cdp-integration-form',
    templateUrl: 'http-integration-form.component.html',
    styleUrls: ['./http-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TMobileIotIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TMobileIotIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class TMobileIotIntegrationFormComponent extends HttpIntegrationFormComponent {

  showSecurity = false;

  protected integrationType = IntegrationType.TMOBILE_IOT_CDP;

  constructor(protected fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              protected translate: TranslateService) {
    super(fb, store, translate);
  }

  ngOnInit() {
    super.ngOnInit();
    this.baseHttpIntegrationConfigForm.removeControl('enableSecurity', {emitEvent: false});
    this.baseHttpIntegrationConfigForm.removeControl('headersFilter', {emitEvent: false});
  }
}

