///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { ActionButtonLinkType, ActionButtonLinkTypeTranslateMap } from '@shared/models/notification.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-notification-action-button-configuration',
  templateUrl: './notification-action-button-configuration.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NotificationActionButtonConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => NotificationActionButtonConfigurationComponent),
      multi: true,
    }
  ]
})
export class NotificationActionButtonConfigurationComponent implements ControlValueAccessor, Validator, OnInit, OnDestroy {

  @Input()
  actionTitle: string;

  @Input()
  sliderHint: string;

  private hideButtonTextValue = false;

  get hideButtonText(): boolean {
    return this.hideButtonTextValue;
  }

  @Input()
  @coerceBoolean()
  set hideButtonText(hideButtonText: boolean) {
    this.hideButtonTextValue = hideButtonText;
    if (this.hideButtonTextValue) {
      this.actionButtonConfigForm.removeControl('text');
    }
  }

  actionButtonConfigForm: FormGroup;

  readonly actionButtonLinkType = ActionButtonLinkType;
  readonly actionButtonLinkTypes = Object.keys(ActionButtonLinkType) as ActionButtonLinkType[];
  readonly actionButtonLinkTypeTranslateMap = ActionButtonLinkTypeTranslateMap;

  private propagateChange = (v: any) => { };
  private readonly destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.actionButtonConfigForm = this.fb.group({
      enabled: [false],
      text: [{value: '', disabled: true}, [Validators.required, Validators.maxLength(50)]],
      linkType: [ActionButtonLinkType.LINK],
      link: [{value: '', disabled: true}, [Validators.required, Validators.maxLength(300)]],
      dashboardId: [{value: null, disabled: true}, Validators.required],
      dashboardState: [{value: null, disabled: true}],
      setEntityIdInState: [{value: true, disabled: true}]
    });


    this.actionButtonConfigForm.get('enabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (value) {
        if (!this.hideButtonText) {
          this.actionButtonConfigForm.get('text').enable({emitEvent: false});
        }
        this.actionButtonConfigForm.get('linkType').enable({onlySelf: false});
      } else {
        this.actionButtonConfigForm.disable({emitEvent: false});
        this.actionButtonConfigForm.get('enabled').enable({emitEvent: false});
      }
    });

    this.actionButtonConfigForm.get('linkType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const isEnabled = this.actionButtonConfigForm.get('enabled').value;
      if (isEnabled) {
        if (value === ActionButtonLinkType.LINK) {
          this.actionButtonConfigForm.get('link').enable({emitEvent: false});
          this.actionButtonConfigForm.get('dashboardId').disable({emitEvent: false});
          this.actionButtonConfigForm.get('dashboardState').disable({emitEvent: false});
          this.actionButtonConfigForm.get('setEntityIdInState').disable({emitEvent: false});
        } else {
          this.actionButtonConfigForm.get('link').disable({emitEvent: false});
          this.actionButtonConfigForm.get('dashboardId').enable({emitEvent: false});
          this.actionButtonConfigForm.get('dashboardState').enable({emitEvent: false});
          this.actionButtonConfigForm.get('setEntityIdInState').enable({emitEvent: false});
        }
      }
    });

    this.actionButtonConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.propagateChange(value));
  }

  ngOnInit() {
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.actionButtonConfigForm.disable({emitEvent: false});
    } else {
      this.actionButtonConfigForm.enable({emitEvent: false});
      this.actionButtonConfigForm.get('enabled').updateValueAndValidity({onlySelf: true});
    }
  }

  validate(): ValidationErrors | null {
    return this.actionButtonConfigForm.valid ? null : {
      actionButtonConfigForm: {
        valid: false
      }
    };
  }

  writeValue(obj) {
    if (isDefinedAndNotNull(obj)) {
      this.actionButtonConfigForm.patchValue(obj, {emitEvent: false});
      this.actionButtonConfigForm.get('enabled').updateValueAndValidity({onlySelf: true});
    }
  }

}
