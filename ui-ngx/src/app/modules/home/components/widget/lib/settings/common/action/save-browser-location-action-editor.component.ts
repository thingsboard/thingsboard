// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {
  defaultSaveBrowserLocationDescriptor,
  getLocationKeys,
  SaveBrowserLocationDescriptor
} from '@shared/models/location.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-save-browser-location-action-editor',
  templateUrl: './save-browser-location-action-editor.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SaveBrowserLocationActionEditorComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SaveBrowserLocationActionEditorComponent),
      multi: true
    }
  ],
  standalone: false
})
export class SaveBrowserLocationActionEditorComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  disabled: boolean;

  @Input()
  callbacks: WidgetActionCallbacks;

  formGroup: FormGroup;

  getLocationKeys = getLocationKeys;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    const defaultDescriptor = defaultSaveBrowserLocationDescriptor();
    this.formGroup = this.fb.group({
      targetEntity: [defaultDescriptor.targetEntity],
      keys: [defaultDescriptor.keys]
    });

    this.formGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.propagateChange(this.formGroup.valid ? this.formGroup.getRawValue() : null));
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.formGroup.disable({emitEvent: false});
    } else {
      this.formGroup.enable({emitEvent: false});
    }
  }

  writeValue(value?: SaveBrowserLocationDescriptor): void {
    const defaultDescriptor = defaultSaveBrowserLocationDescriptor();
    this.formGroup.patchValue({
      targetEntity: value?.targetEntity ?? defaultDescriptor.targetEntity,
      keys: value?.keys?.length ? value.keys : defaultDescriptor.keys
    }, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.formGroup.valid ? null : {
      saveBrowserLocation: {
        valid: false
      }
    };
  }
}
