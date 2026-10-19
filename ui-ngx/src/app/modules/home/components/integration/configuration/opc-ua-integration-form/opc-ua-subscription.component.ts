// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { OpcUaSubscription } from '@shared/models/integration.models';

@Component({
    selector: 'tb-opc-ua-subscription',
    templateUrl: './opc-ua-subscription.component.html',
    styleUrls: ['./opc-ua-subscription.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OpcUaSubscriptionComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => OpcUaSubscriptionComponent),
            multi: true,
        }],
    standalone: false
})
export class OpcUaSubscriptionComponent implements ControlValueAccessor, Validator, OnDestroy {

  opcSubscriptionForm: UntypedFormGroup;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.opcSubscriptionForm = this.fb.group({
      subscription: this.fb.array([], Validators.required)
    });
    this.opcSubscriptionForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.updateModels(value.subscription);
    });
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
      this.opcSubscriptionForm.disable({emitEvent: false});
    } else {
      this.opcSubscriptionForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.opcSubscriptionForm.valid && this.opcSubscriptionArray.length ? null : {
      opcSubscriptionForm: {valid: false}
    };
  }

  writeValue(subscriptions: OpcUaSubscription[]) {
    if (isDefinedAndNotNull(subscriptions)) {
      if (this.opcSubscriptionArray.length === subscriptions.length) {
        this.opcSubscriptionForm.get('subscription').patchValue(subscriptions, {emitEvent: false});
      } else {
        const subscriptionControls: Array<UntypedFormGroup> = [];
        subscriptions.forEach((subscription) => {
          subscriptionControls.push(this.createFormGroup(subscription));
        });
        this.opcSubscriptionForm.setControl('subscription', this.fb.array(subscriptionControls), {emitEvent: false});
        if (this.disabled) {
          this.opcSubscriptionForm.disable({emitEvent: false});
        }
      }
    } else {
      this.addSubscriptionTag(false);
    }
  }

  get opcSubscriptionArray(): UntypedFormArray {
    return this.opcSubscriptionForm.get('subscription') as UntypedFormArray;
  }

  get opcSubscriptionArrayControls(): UntypedFormGroup[] {
    return this.opcSubscriptionArray.controls as UntypedFormGroup[];
  }

  addSubscriptionTag(emitEvent = true) {
    this.opcSubscriptionArray.push(this.createFormGroup(), {emitEvent});
  }

  private createFormGroup(value?: any): UntypedFormGroup {
    return this.fb.group(
      {
        key: [value?.key || '', [Validators.required]],
        path: [value?.path || '', [Validators.required]],
        required: [value?.required || false]
      }
    );
  }

  removeSubscriptionTag(index: number) {
    this.opcSubscriptionArray.removeAt(index);
  }

  private updateModels(value) {
    this.propagateChange(value);
  }
}
