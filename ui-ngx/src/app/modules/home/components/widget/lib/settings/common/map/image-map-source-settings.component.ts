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
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ImageMapSourceSettings, ImageSourceType } from '@shared/models/widget/maps/map.models';
import { DataKey, DatasourceType, widgetType } from '@shared/models/widget.models';
import { MapSettingsContext } from '@home/components/widget/lib/settings/common/map/map-settings.component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

@Component({
    selector: 'tb-image-map-source-settings',
    templateUrl: './image-map-source-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ImageMapSourceSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ImageMapSourceSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ImageMapSourceSettingsComponent implements OnInit, ControlValueAccessor, Validator {

  ImageSourceType = ImageSourceType;
  DatasourceType = DatasourceType;
  widgetType = widgetType;
  DataKeyType = DataKeyType;

  @Input()
  disabled: boolean;

  @Input()
  context: MapSettingsContext;

  private modelValue: ImageMapSourceSettings;

  private propagateChange = null;

  public imageMapSourceFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.imageMapSourceFormGroup = this.fb.group({
      sourceType: [null, [Validators.required]],
      url: [null, [Validators.required]],
      entityAliasId: [null, [Validators.required]],
      entityKey: [null, [Validators.required]]
    });
    this.imageMapSourceFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.imageMapSourceFormGroup.get('sourceType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.imageMapSourceFormGroup.disable({emitEvent: false});
    } else {
      this.imageMapSourceFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ImageMapSourceSettings): void {
    this.modelValue = value;
    this.imageMapSourceFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators();
  }

  editKey() {
    const entityKey: DataKey = this.imageMapSourceFormGroup.get('entityKey').value;
    this.context.editKey(entityKey,
      null, this.imageMapSourceFormGroup.get('entityAliasId').value).subscribe(
      (updatedDataKey) => {
        if (updatedDataKey) {
          this.imageMapSourceFormGroup.get('entityKey').patchValue(updatedDataKey);
        }
      }
    );
  }

  public validate(c: UntypedFormControl) {
    const valid = this.imageMapSourceFormGroup.valid;
    return valid ? null : {
      imageMapSource: {
        valid: false,
      },
    };
  }


  private updateValidators() {
    const sourceType: ImageSourceType = this.imageMapSourceFormGroup.get('sourceType').value;
    if (sourceType === ImageSourceType.image) {
      this.imageMapSourceFormGroup.get('url').enable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityAliasId').disable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityKey').disable({emitEvent: false});
    } else {
      this.imageMapSourceFormGroup.get('url').disable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityAliasId').enable({emitEvent: false});
      this.imageMapSourceFormGroup.get('entityKey').enable({emitEvent: false});
    }
  }

  private updateModel() {
    this.modelValue = this.imageMapSourceFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
