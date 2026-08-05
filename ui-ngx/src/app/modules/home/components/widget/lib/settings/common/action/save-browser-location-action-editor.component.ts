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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  defaultLocationKeyMappings,
  getLocationKeys,
  MobileActionAttributeSource,
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
    }
  ],
  standalone: false
})
export class SaveBrowserLocationActionEditorComponent implements ControlValueAccessor, OnInit {

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
    this.formGroup = this.fb.group({
      targetEntity: [{type: MobileActionAttributeSource.CURRENT_ENTITY}],
      keys: [defaultLocationKeyMappings()]
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
    this.formGroup.patchValue({
      targetEntity: value?.targetEntity ?? {type: MobileActionAttributeSource.CURRENT_ENTITY},
      keys: value?.keys?.length ? value.keys : defaultLocationKeyMappings()
    }, {emitEvent: false});
  }
}
