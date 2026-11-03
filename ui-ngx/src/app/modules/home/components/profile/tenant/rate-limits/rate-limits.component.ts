// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import {
  RateLimitsDetailsDialogComponent,
  RateLimitsDetailsDialogData
} from '@home/components/profile/tenant/rate-limits/rate-limits-details-dialog.component';
import {
  RateLimits,
  rateLimitsDialogTitleTranslationMap,
  rateLimitsLabelTranslationMap,
  RateLimitsType,
  stringToRateLimitsArray
} from './rate-limits.models';
import { isDefined } from '@core/utils';

@Component({
    selector: 'tb-rate-limits',
    templateUrl: './rate-limits.component.html',
    styleUrls: ['./rate-limits.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RateLimitsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => RateLimitsComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class RateLimitsComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  type: RateLimitsType;

  label: string;

  rateLimitsFormGroup: UntypedFormGroup;

  get rateLimitsArray(): Array<RateLimits> {
    return this.rateLimitsFormGroup.get('rateLimits').value;
  }

  private modelValue: string;

  private propagateChange = null;

  constructor(private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.label = rateLimitsLabelTranslationMap.get(this.type);
    this.rateLimitsFormGroup = this.fb.group({
      rateLimits: [null, []]
    });
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.rateLimitsFormGroup.disable({emitEvent: false});
    } else {
      this.rateLimitsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string) {
    this.modelValue = value;
    this.updateRateLimitsInfo();
  }

  public validate(c: UntypedFormControl) {
    return null;
  }

  public onClick($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = rateLimitsDialogTitleTranslationMap.get(this.type);
    this.dialog.open<RateLimitsDetailsDialogComponent, RateLimitsDetailsDialogData,
      string>(RateLimitsDetailsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        rateLimits: this.modelValue,
        title,
        readonly: this.disabled
      }
    }).afterClosed().subscribe((result) => {
      if (isDefined(result)) {
        this.modelValue = result;
        this.updateModel();
      }
    });
  }

  private updateRateLimitsInfo() {
    this.rateLimitsFormGroup.patchValue(
      {
        rateLimits: stringToRateLimitsArray(this.modelValue)
      }
    );
  }

  private updateModel() {
    this.updateRateLimitsInfo();
    this.propagateChange(this.modelValue);
  }

}
