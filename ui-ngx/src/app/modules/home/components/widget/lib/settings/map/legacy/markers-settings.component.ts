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

import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator, Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  MapProviders,
  MarkersSettings, ShowTooltipAction, showTooltipActionTranslationMap
} from '@home/components/widget/lib/maps-legacy/map-models';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-markers-settings',
  templateUrl: './markers-settings.component.html',
  styleUrls: ['./../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MarkersSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MarkersSettingsComponent),
      multi: true
    }
  ]
})
export class MarkersSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator, OnChanges {

  @Input()
  disabled: boolean;

  @Input()
  provider: MapProviders;

  mapProvider = MapProviders;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: MarkersSettings;

  private propagateChange = null;

  public markersSettingsFormGroup: UntypedFormGroup;

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
    this.markersSettingsFormGroup = this.fb.group({
      markerOffsetX: [null, []],
      markerOffsetY: [null, []],
      posFunction: [null, []],
      draggableMarker: [null, []],
      showLabel: [null, []],
      useLabelFunction: [null, []],
      label: [null, []],
      labelFunction: [null, []],
      showTooltip: [null, []],
      showTooltipAction: [null, []],
      autocloseTooltip: [null, []],
      useTooltipFunction: [null, []],
      tooltipPattern: [null, []],
      tooltipFunction: [null, []],
      tooltipOffsetX: [null, []],
      tooltipOffsetY: [null, []],
      color: [null, []],
      useColorFunction: [null, []],
      colorFunction: [null, []],
      useMarkerImageFunction: [null, []],
      markerImage: [null, []],
      markerImageSize: [null, [Validators.min(1)]],
      markerImageFunction: [null, []],
      markerImages: [null, []]
    });
    this.markersSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.markersSettingsFormGroup.get('showLabel').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.markersSettingsFormGroup.get('useLabelFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.markersSettingsFormGroup.get('showTooltip').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.markersSettingsFormGroup.get('useTooltipFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.markersSettingsFormGroup.get('useColorFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.markersSettingsFormGroup.get('useMarkerImageFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'provider') {
          this.updateValidators(false);
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.markersSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.markersSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MarkersSettings): void {
    this.modelValue = value;
    this.markersSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: UntypedFormControl) {
    return this.markersSettingsFormGroup.valid ? null : {
      markersSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: MarkersSettings = this.markersSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showLabel: boolean = this.markersSettingsFormGroup.get('showLabel').value;
    const useLabelFunction: boolean = this.markersSettingsFormGroup.get('useLabelFunction').value;
    const showTooltip: boolean = this.markersSettingsFormGroup.get('showTooltip').value;
    const useTooltipFunction: boolean = this.markersSettingsFormGroup.get('useTooltipFunction').value;
    const useColorFunction: boolean = this.markersSettingsFormGroup.get('useColorFunction').value;
    const useMarkerImageFunction: boolean = this.markersSettingsFormGroup.get('useMarkerImageFunction').value;
    if (this.provider === MapProviders.image) {
      this.markersSettingsFormGroup.get('posFunction').enable({emitEvent});
    } else {
      this.markersSettingsFormGroup.get('posFunction').disable({emitEvent});
    }
    if (showLabel) {
      this.markersSettingsFormGroup.get('useLabelFunction').enable({emitEvent: false});
      if (useLabelFunction) {
        this.markersSettingsFormGroup.get('labelFunction').enable({emitEvent});
        this.markersSettingsFormGroup.get('label').disable({emitEvent});
      } else {
        this.markersSettingsFormGroup.get('labelFunction').disable({emitEvent});
        this.markersSettingsFormGroup.get('label').enable({emitEvent});
      }
    } else {
      this.markersSettingsFormGroup.get('useLabelFunction').disable({emitEvent: false});
      this.markersSettingsFormGroup.get('labelFunction').disable({emitEvent});
      this.markersSettingsFormGroup.get('label').disable({emitEvent});
    }
    if (showTooltip) {
      this.markersSettingsFormGroup.get('showTooltipAction').enable({emitEvent});
      this.markersSettingsFormGroup.get('autocloseTooltip').enable({emitEvent});
      this.markersSettingsFormGroup.get('useTooltipFunction').enable({emitEvent: false});
      this.markersSettingsFormGroup.get('tooltipOffsetX').enable({emitEvent});
      this.markersSettingsFormGroup.get('tooltipOffsetY').enable({emitEvent});
      if (useTooltipFunction) {
        this.markersSettingsFormGroup.get('tooltipFunction').enable({emitEvent});
        this.markersSettingsFormGroup.get('tooltipPattern').disable({emitEvent});
      } else {
        this.markersSettingsFormGroup.get('tooltipFunction').disable({emitEvent});
        this.markersSettingsFormGroup.get('tooltipPattern').enable({emitEvent});
      }
    } else {
      this.markersSettingsFormGroup.get('showTooltipAction').disable({emitEvent});
      this.markersSettingsFormGroup.get('autocloseTooltip').disable({emitEvent});
      this.markersSettingsFormGroup.get('useTooltipFunction').disable({emitEvent: false});
      this.markersSettingsFormGroup.get('tooltipFunction').disable({emitEvent});
      this.markersSettingsFormGroup.get('tooltipPattern').disable({emitEvent});
      this.markersSettingsFormGroup.get('tooltipOffsetX').disable({emitEvent});
      this.markersSettingsFormGroup.get('tooltipOffsetY').disable({emitEvent});
    }
    if (useColorFunction) {
      this.markersSettingsFormGroup.get('colorFunction').enable({emitEvent});
    } else {
      this.markersSettingsFormGroup.get('colorFunction').disable({emitEvent});
    }
    if (useMarkerImageFunction) {
      this.markersSettingsFormGroup.get('markerImageFunction').enable({emitEvent});
      this.markersSettingsFormGroup.get('markerImages').enable({emitEvent});
      this.markersSettingsFormGroup.get('markerImage').disable({emitEvent});
      this.markersSettingsFormGroup.get('markerImageSize').disable({emitEvent});
    } else {
      this.markersSettingsFormGroup.get('markerImageFunction').disable({emitEvent});
      this.markersSettingsFormGroup.get('markerImages').disable({emitEvent});
      this.markersSettingsFormGroup.get('markerImage').enable({emitEvent});
      this.markersSettingsFormGroup.get('markerImageSize').enable({emitEvent});
    }
    this.markersSettingsFormGroup.get('posFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('useLabelFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('labelFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('label').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('showTooltipAction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('autocloseTooltip').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('useTooltipFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('tooltipFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('tooltipPattern').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('tooltipOffsetX').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('tooltipOffsetY').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('colorFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('markerImageFunction').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('markerImages').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('markerImage').updateValueAndValidity({emitEvent: false});
    this.markersSettingsFormGroup.get('markerImageSize').updateValueAndValidity({emitEvent: false});
  }
}
