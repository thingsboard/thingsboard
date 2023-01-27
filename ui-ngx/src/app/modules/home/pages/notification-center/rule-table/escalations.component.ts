///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Subscription } from 'rxjs';
import { QueueInfo } from '@shared/models/queue.models';
import { UtilsService } from '@core/services/utils.service';
import { guid } from '@core/utils';
import { NonConfirmedNotificationEscalation } from '@shared/models/notification.models';

@Component({
  selector: 'tb-escalations-component',
  templateUrl: './escalations.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EscalationsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EscalationsComponent),
      multi: true,
    }
  ]
})
export class EscalationsComponent implements ControlValueAccessor, Validator, OnDestroy {

  escalationsFormGroup: FormGroup;
  newEscalation = false;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private mainEscalaion = {
    delayInSec: null,
    notificationTargetId: null
  };

  private valueChangeSubscription$: Subscription = null;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private utils: UtilsService,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  ngOnDestroy() {
    if (this.valueChangeSubscription$) {
      this.valueChangeSubscription$.unsubscribe();
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.escalationsFormGroup = this.fb.group({
      escalations: this.fb.array([])
    });
  }

  get escalationsFormArray(): FormArray {
    return this.escalationsFormGroup.get('escalations') as FormArray;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.escalationsFormGroup.disable({emitEvent: false});
    } else {
      this.escalationsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(escalations: Array<NonConfirmedNotificationEscalation> | null): void {
    if (this.valueChangeSubscription$) {
      this.valueChangeSubscription$.unsubscribe();
    }
    const escalationsControls: Array<AbstractControl> = [];
    if (escalations) {
      escalations.forEach((escalation, index) => {
        escalationsControls.push(this.fb.control(escalation, [Validators.required]));
      });
    } else {
      escalationsControls.push(this.fb.control(this.mainEscalaion, [Validators.required]));
    }
    this.escalationsFormGroup.setControl('escalations', this.fb.array(escalationsControls));
    if (this.disabled) {
      this.escalationsFormGroup.disable({emitEvent: false});
    } else {
      this.escalationsFormGroup.enable({emitEvent: false});
    }
    this.valueChangeSubscription$ = this.escalationsFormGroup.valueChanges.subscribe(() =>
      this.updateModel()
    );
  }

  public removeEscalation(index: number) {
    (this.escalationsFormGroup.get('escalations') as FormArray).removeAt(index);
  }

  public addEscalation() {
    const escalation: NonConfirmedNotificationEscalation = {
      delayInSec: null,
      notificationTargetId: null
    };
    this.newEscalation = true;
    const escalationArray = this.escalationsFormGroup.get('escalations') as FormArray;
    escalationArray.push(this.fb.control(escalation, []));
    this.escalationsFormGroup.updateValueAndValidity();
    if (!this.escalationsFormGroup.valid) {
      this.updateModel();
    }
  }

  public validate(c: AbstractControl): ValidationErrors | null {
    return this.escalationsFormGroup.valid ? null : {
      escalation: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const escalations: Array<NonConfirmedNotificationEscalation> = this.escalationsFormGroup.get('escalations').value;
    this.propagateChange(escalations);
  }
}
