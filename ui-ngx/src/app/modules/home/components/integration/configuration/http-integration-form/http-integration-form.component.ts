// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { baseUrl, isDefinedAndNotNull } from '@core/utils';
import { takeUntil } from 'rxjs/operators';
import { HttpIntegration, IntegrationType } from '@shared/models/integration.models';
import {
  integrationEndPointUrl,
  privateNetworkAddressValidator
} from '@home/components/integration/integration.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';

@Component({
    selector: 'tb-http-integration-form',
    templateUrl: './http-integration-form.component.html',
    styleUrls: ['./http-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => HttpIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => HttpIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class HttpIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  baseHttpIntegrationConfigForm: UntypedFormGroup;
  showSecurity = true;

  @Input()
  routingKey: string;

  protected integrationType = IntegrationType.HTTP;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(protected fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              protected translate: TranslateService) {
    super();
  }

  ngOnInit() {
    const baseURLValidators = [Validators.required];
    if (!this.allowLocalNetwork) {
      baseURLValidators.push(privateNetworkAddressValidator);
    }
    this.baseHttpIntegrationConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey), disabled: true}],
      enableSecurity: [false],
      headersFilter: [{}],
      replaceNoContentToOk: [false]
    });
    this.baseHttpIntegrationConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.baseHttpIntegrationConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
    this.baseHttpIntegrationConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.baseHttpIntegrationConfigForm.getRawValue());
    });
  }

  writeValue(value: HttpIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.baseHttpIntegrationConfigForm.reset(value, {emitEvent: false});
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.baseHttpIntegrationConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.baseHttpIntegrationConfigForm.disable({emitEvent: false});
    } else {
      this.baseHttpIntegrationConfigForm.enable({emitEvent: false});
      this.baseHttpIntegrationConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.baseHttpIntegrationConfigForm.valid ? null : {
      baseHttpIntegrationConfigForm: {valid: false}
    };
  }

  onHttpEndpointCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('integration.http-endpoint-url-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left',
        target: 'integrationRoot'
      }));
  }

  updatedValidationPrivateNetwork() {
    if (this.allowLocalNetwork) {
      this.baseHttpIntegrationConfigForm?.get('baseUrl').removeValidators(privateNetworkAddressValidator);
    } else {
      this.baseHttpIntegrationConfigForm?.get('baseUrl').addValidators(privateNetworkAddressValidator);
    }
    this.baseHttpIntegrationConfigForm?.get('baseUrl').updateValueAndValidity({emitEvent: false});
  }
}
