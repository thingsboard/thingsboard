// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef } from '@angular/core';
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
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import {
  AwsKinesisIntegration,
  InitialPositionInStream,
  InitialPositionInStreamTranslation
} from '@shared/models/integration.models';

@Component({
    selector: 'tb-aws-kinesis-integration-form',
    templateUrl: './aws-kinesis-integration-form.component.html',
    styleUrls: ['./aws-kinesis-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AwsKinesisIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => AwsKinesisIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class AwsKinesisIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  awsKinesisConfigForm: UntypedFormGroup;

  initialPositionInStreams = Object.keys(InitialPositionInStream);
  InitialPositionInStreamTranslation = InitialPositionInStreamTranslation;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    super();
    this.awsKinesisConfigForm = this.fb.group({
      streamName: ['', Validators.required],
      region: ['', Validators.required],
      accessKeyId: ['', Validators.required],
      secretAccessKey: ['', Validators.required],
      useCredentialsFromInstanceMetadata: [false],
      applicationName: [''],
      initialPositionInStream: ['', Validators.required],
      useConsumersWithEnhancedFanOut: [false],
      maxRecords: [10000, [Validators.required, Validators.min(1), Validators.max(10000)]],
      requestTimeout: [30, Validators.required]
    });

    this.awsKinesisConfigForm.get('useCredentialsFromInstanceMetadata').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateUsedMetadata(value);
    });
    this.awsKinesisConfigForm.get('useConsumersWithEnhancedFanOut').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateEnhancedFanOut(value);
    });
    this.awsKinesisConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.awsKinesisConfigForm.getRawValue());
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched() { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.awsKinesisConfigForm.disable({emitEvent: false});
    } else {
      this.awsKinesisConfigForm.enable({emitEvent: false});
      this.awsKinesisConfigForm.get('useCredentialsFromInstanceMetadata').updateValueAndValidity({onlySelf: true});
      this.awsKinesisConfigForm.get('useConsumersWithEnhancedFanOut').updateValueAndValidity({onlySelf: true});
    }
  }

  validate(): ValidationErrors | null {
    return this.awsKinesisConfigForm.valid ? null : {
      awsKinesisIntegrationConfigForm: {valid: false}
    };
  }

  writeValue(value: AwsKinesisIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.awsKinesisConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.awsKinesisConfigForm.get('useCredentialsFromInstanceMetadata').updateValueAndValidity({onlySelf: true});
        this.awsKinesisConfigForm.get('useConsumersWithEnhancedFanOut').updateValueAndValidity({onlySelf: true});
      }
    }
  }

  private updateUsedMetadata(value: boolean) {
    if (value) {
      this.awsKinesisConfigForm.get('accessKeyId').disable({emitEvent: false});
      this.awsKinesisConfigForm.get('secretAccessKey').disable({emitEvent: false});
    } else {
      this.awsKinesisConfigForm.get('accessKeyId').enable({emitEvent: false});
      this.awsKinesisConfigForm.get('secretAccessKey').enable({emitEvent: false});
    }
  }

  private updateEnhancedFanOut(value: boolean) {
    if (value) {
      this.awsKinesisConfigForm.get('maxRecords').disable({emitEvent: false});
      this.awsKinesisConfigForm.get('requestTimeout').disable({emitEvent: false});
    } else {
      this.awsKinesisConfigForm.get('maxRecords').enable({emitEvent: false});
      this.awsKinesisConfigForm.get('requestTimeout').enable({emitEvent: false});
    }
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  getInitialPositionInStreamTranslation (stream: string) {
    return this.InitialPositionInStreamTranslation.get(stream as InitialPositionInStream);
  }
}
