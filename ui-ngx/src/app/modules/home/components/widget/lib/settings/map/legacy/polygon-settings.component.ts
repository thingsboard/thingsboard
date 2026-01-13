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
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  PolygonSettings,
  ShowTooltipAction,
  showTooltipActionTranslationMap
} from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { Widget } from '@shared/models/widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-polygon-settings',
  templateUrl: './polygon-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PolygonSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PolygonSettingsComponent),
      multi: true
    }
  ]
})
export class PolygonSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  widget: Widget;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: PolygonSettings;

  private propagateChange = null;

  public polygonSettingsFormGroup: UntypedFormGroup;

  showTooltipActions = Object.values(ShowTooltipAction);

  showTooltipActionTranslations = showTooltipActionTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.polygonSettingsFormGroup = this.fb.group({
      showPolygon: [null, []],
      polygonKeyName: [null, [Validators.required]],
      editablePolygon: [null, []],
      showPolygonLabel: [null, []],
      usePolygonLabelFunction: [null, []],
      polygonLabel: [null, []],
      polygonLabelFunction: [null, []],
      showPolygonTooltip: [null, []],
      showPolygonTooltipAction: [null, []],
      autoClosePolygonTooltip: [null, []],
      usePolygonTooltipFunction: [null, []],
      polygonTooltipPattern: [null, []],
      polygonTooltipFunction: [null, []],
      polygonColor: [null, []],
      polygonOpacity: [null, [Validators.min(0), Validators.max(1)]],
      usePolygonColorFunction: [null, []],
      polygonColorFunction: [null, []],
      polygonStrokeColor: [null, []],
      polygonStrokeOpacity: [null, [Validators.min(0), Validators.max(1)]],
      polygonStrokeWeight: [null, [Validators.min(0)]],
      usePolygonStrokeColorFunction: [null, []],
      polygonStrokeColorFunction: [null, []]
    });
    this.polygonSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.polygonSettingsFormGroup.get('showPolygon').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.polygonSettingsFormGroup.get('showPolygonLabel').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.polygonSettingsFormGroup.get('usePolygonLabelFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.polygonSettingsFormGroup.get('showPolygonTooltip').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.polygonSettingsFormGroup.get('usePolygonTooltipFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.polygonSettingsFormGroup.get('usePolygonColorFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.polygonSettingsFormGroup.get('usePolygonStrokeColorFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.polygonSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.polygonSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: PolygonSettings): void {
    this.modelValue = value;
    this.polygonSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.polygonSettingsFormGroup.valid ? null : {
      polygonSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: PolygonSettings = this.polygonSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showPolygon: boolean = this.polygonSettingsFormGroup.get('showPolygon').value;
    const showPolygonLabel: boolean = this.polygonSettingsFormGroup.get('showPolygonLabel').value;
    const usePolygonLabelFunction: boolean = this.polygonSettingsFormGroup.get('usePolygonLabelFunction').value;
    const showPolygonTooltip: boolean = this.polygonSettingsFormGroup.get('showPolygonTooltip').value;
    const usePolygonTooltipFunction: boolean = this.polygonSettingsFormGroup.get('usePolygonTooltipFunction').value;
    const usePolygonColorFunction: boolean = this.polygonSettingsFormGroup.get('usePolygonColorFunction').value;
    const usePolygonStrokeColorFunction: boolean = this.polygonSettingsFormGroup.get('usePolygonStrokeColorFunction').value;

    this.polygonSettingsFormGroup.disable({emitEvent: false});
    this.polygonSettingsFormGroup.get('showPolygon').enable({emitEvent: false});

    if (showPolygon) {
      this.polygonSettingsFormGroup.get('polygonKeyName').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('editablePolygon').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('showPolygonLabel').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('showPolygonTooltip').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('polygonColor').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('polygonOpacity').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('usePolygonColorFunction').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('polygonStrokeColor').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('polygonStrokeOpacity').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('polygonStrokeWeight').enable({emitEvent: false});
      this.polygonSettingsFormGroup.get('usePolygonStrokeColorFunction').enable({emitEvent: false});
      if (showPolygonLabel) {
        this.polygonSettingsFormGroup.get('usePolygonLabelFunction').enable({emitEvent: false});
        if (usePolygonLabelFunction) {
          this.polygonSettingsFormGroup.get('polygonLabelFunction').enable({emitEvent});
          this.polygonSettingsFormGroup.get('polygonLabel').disable({emitEvent});
        } else {
          this.polygonSettingsFormGroup.get('polygonLabelFunction').disable({emitEvent});
          this.polygonSettingsFormGroup.get('polygonLabel').enable({emitEvent});
        }
      } else {
        this.polygonSettingsFormGroup.get('usePolygonLabelFunction').disable({emitEvent: false});
        this.polygonSettingsFormGroup.get('polygonLabelFunction').disable({emitEvent});
        this.polygonSettingsFormGroup.get('polygonLabel').disable({emitEvent});
      }
      if (showPolygonTooltip) {
        this.polygonSettingsFormGroup.get('showPolygonTooltipAction').enable({emitEvent});
        this.polygonSettingsFormGroup.get('autoClosePolygonTooltip').enable({emitEvent});
        this.polygonSettingsFormGroup.get('usePolygonTooltipFunction').enable({emitEvent: false});
        if (usePolygonTooltipFunction) {
          this.polygonSettingsFormGroup.get('polygonTooltipFunction').enable({emitEvent});
          this.polygonSettingsFormGroup.get('polygonTooltipPattern').disable({emitEvent});
        } else {
          this.polygonSettingsFormGroup.get('polygonTooltipFunction').disable({emitEvent});
          this.polygonSettingsFormGroup.get('polygonTooltipPattern').enable({emitEvent});
        }
      } else {
        this.polygonSettingsFormGroup.get('showPolygonTooltipAction').disable({emitEvent});
        this.polygonSettingsFormGroup.get('autoClosePolygonTooltip').disable({emitEvent});
        this.polygonSettingsFormGroup.get('usePolygonTooltipFunction').disable({emitEvent: false});
        this.polygonSettingsFormGroup.get('polygonTooltipFunction').disable({emitEvent});
        this.polygonSettingsFormGroup.get('polygonTooltipPattern').disable({emitEvent});
      }
      if (usePolygonColorFunction) {
        this.polygonSettingsFormGroup.get('polygonColorFunction').enable({emitEvent});
      } else {
        this.polygonSettingsFormGroup.get('polygonColorFunction').disable({emitEvent});
      }
      if (usePolygonStrokeColorFunction) {
        this.polygonSettingsFormGroup.get('polygonStrokeColorFunction').enable({emitEvent});
      } else {
        this.polygonSettingsFormGroup.get('polygonStrokeColorFunction').disable({emitEvent});
      }
    }
    this.polygonSettingsFormGroup.updateValueAndValidity({emitEvent: false});
  }
}
