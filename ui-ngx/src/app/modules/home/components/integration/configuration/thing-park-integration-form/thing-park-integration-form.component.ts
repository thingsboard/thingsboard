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
import { IntegrationType, ThingParkIntegration } from '@shared/models/integration.models';
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
    selector: 'tb-thing-spark-integration-form',
    templateUrl: './thing-park-integration-form.component.html',
    styleUrls: ['./thing-park-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ThingParkIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ThingParkIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class ThingParkIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator, OnInit {

  thingParkConfigForm: UntypedFormGroup;

  @Input()
  routingKey: string;

  protected integrationType = IntegrationType.THINGPARK;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };

  constructor(protected fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              protected translate: TranslateService) {
    super();
  }

  ngOnInit() {
    const baseURLValidators = [Validators.required];
    const downlinkUrlValidators = [];
    if (!this.allowLocalNetwork) {
      baseURLValidators.push(privateNetworkAddressValidator);
      downlinkUrlValidators.push(privateNetworkAddressValidator);
    }
    this.thingParkConfigForm = this.fb.group({
      baseUrl: [baseUrl(), baseURLValidators],
      httpEndpoint: [{value: integrationEndPointUrl(this.integrationType, baseUrl(), this.routingKey), disabled: true}],
      enableSecurity: [false],
      replaceNoContentToOk: [false],
      downlinkUrl: ['https://api.thingpark.com/thingpark/lrc/rest/downlink', downlinkUrlValidators],
      enableSecurityNew: [{value: false, disabled: true}],
      asId: [{value: '', disabled: true}, Validators.required],
      asIdNew: [{value: '', disabled: true}, Validators.required],
      asKey: [{value: '', disabled: true}, Validators.required],
      clientIdNew: [{value: '', disabled: true}, Validators.required],
      clientSecret: [{value: '', disabled: true}, Validators.required],
      maxTimeDiffInSeconds: [{value: 60, disabled: true}, [Validators.required, Validators.min(0)]]
    });
    this.thingParkConfigForm.get('baseUrl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const httpEndpoint = integrationEndPointUrl(this.integrationType, value, this.routingKey);
      this.thingParkConfigForm.get('httpEndpoint').patchValue(httpEndpoint);
    });
    this.thingParkConfigForm.get('enableSecurity').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updatedEnabledSecurity(value);
    });
    this.thingParkConfigForm.get('enableSecurityNew').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updatedEnabledSecurityNew(value);
    });
    this.thingParkConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.thingParkConfigForm.getRawValue());
    });
  }

  writeValue(value: ThingParkIntegration) {
    if (isDefinedAndNotNull(value)) {
      this.thingParkConfigForm.patchValue(value, {emitEvent: false});
      if (!this.disabled) {
        this.thingParkConfigForm.get('enableSecurity').updateValueAndValidity({onlySelf: true});
        this.thingParkConfigForm.get('enableSecurityNew').updateValueAndValidity({onlySelf: true});
      }
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.thingParkConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.thingParkConfigForm.disable({emitEvent: false});
    } else {
      this.thingParkConfigForm.enable({emitEvent: false});
      this.thingParkConfigForm.get('enableSecurity').updateValueAndValidity({onlySelf: true});
      this.thingParkConfigForm.get('enableSecurityNew').updateValueAndValidity({onlySelf: true});
      this.thingParkConfigForm.get('httpEndpoint').disable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.thingParkConfigForm.valid ? null : {
      baseHttpIntegrationConfigForm: {valid: false}
    };
  }

  private updatedEnabledSecurity(enable: boolean) {
    if (enable) {
      this.thingParkConfigForm.get('enableSecurityNew').enable({emitEvent: false});
      this.thingParkConfigForm.get('asId').enable({emitEvent: false});
      this.thingParkConfigForm.get('asKey').enable({emitEvent: false});
      this.thingParkConfigForm.get('maxTimeDiffInSeconds').enable({emitEvent: false});
    } else {
      this.thingParkConfigForm.get('enableSecurityNew').disable({emitEvent: false});
      this.thingParkConfigForm.get('asId').disable({emitEvent: false});
      this.thingParkConfigForm.get('asKey').disable({emitEvent: false});
      this.thingParkConfigForm.get('maxTimeDiffInSeconds').disable({emitEvent: false});
    }
  }

  private updatedEnabledSecurityNew(enable: boolean) {
    if (enable) {
      this.thingParkConfigForm.get('clientIdNew').enable({emitEvent: false});
      this.thingParkConfigForm.get('clientSecret').enable({emitEvent: false});
      this.thingParkConfigForm.get('asIdNew').enable({emitEvent: false});
      this.thingParkConfigForm.get('asId').disable({emitEvent: false});
    } else {
      this.thingParkConfigForm.get('clientIdNew').disable({emitEvent: false});
      this.thingParkConfigForm.get('clientSecret').disable({emitEvent: false});
      this.thingParkConfigForm.get('asIdNew').disable({emitEvent: false});
      if (this.thingParkConfigForm.get('enableSecurity').value) {
        this.thingParkConfigForm.get('asId').enable({emitEvent: false});
      }
    }
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
    if (this.thingParkConfigForm) {
      if (this.allowLocalNetwork) {
        this.thingParkConfigForm.get('baseUrl').removeValidators(privateNetworkAddressValidator);
        this.thingParkConfigForm.get('downlinkUrl').removeValidators(privateNetworkAddressValidator);
      } else {
        this.thingParkConfigForm.get('baseUrl').addValidators(privateNetworkAddressValidator);
        this.thingParkConfigForm.get('downlinkUrl').addValidators(privateNetworkAddressValidator);
      }
      this.thingParkConfigForm.get('baseUrl').updateValueAndValidity({emitEvent: false});
      this.thingParkConfigForm.get('downlinkUrl').updateValueAndValidity({emitEvent: false});
    }
  }
}
