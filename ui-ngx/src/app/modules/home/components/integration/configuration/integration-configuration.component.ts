// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnDestroy, TemplateRef, ViewEncapsulation } from '@angular/core';
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
import { IntegrationType } from '@shared/models/integration.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-integration-configuration',
    templateUrl: './integration-configuration.component.html',
    styleUrls: ['./integration-configuration.component.scss'],
    encapsulation: ViewEncapsulation.None,
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => IntegrationConfigurationComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => IntegrationConfigurationComponent),
            multi: true,
        }],
    standalone: false
})
export class IntegrationConfigurationComponent implements ControlValueAccessor, Validator, OnDestroy {

  integrationConfigurationForm: UntypedFormGroup;
  integrationTypes = IntegrationType;

  @Input() executeRemotelyTemplate: TemplateRef<any>;
  @Input() genericAdditionalInfoTemplate: TemplateRef<any>;

  @Input() isSetDownlink: boolean;
  @Input() routingKey: string;
  @Input() integrationType: IntegrationType;
  @Input() isEdgeTemplate: boolean;
  @Input() allowLocalNetwork = true;

  @Input() disabled: boolean;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.integrationConfigurationForm = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.integrationConfigurationForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateModel(value.configuration));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.integrationConfigurationForm.disable({emitEvent: false});
    } else {
      this.integrationConfigurationForm.enable({emitEvent: false});
    }
  }

  writeValue(value: any) {
    this.integrationConfigurationForm.get('configuration').reset(value, {emitEvent: false});
  }

  private updateModel(value: any) {
    this.propagateChange(value);
  }

  validate(): ValidationErrors | null {
    return this.integrationConfigurationForm.valid ? null : {
      integrationConfiguration: {valid: false}
    };
  }
}
