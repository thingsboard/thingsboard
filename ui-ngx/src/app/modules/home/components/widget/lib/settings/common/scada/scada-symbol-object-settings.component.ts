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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  defaultScadaSymbolObjectSettings,
  parseScadaSymbolMetadataFromContent,
  ScadaSymbolBehaviorType,
  ScadaSymbolMetadata,
  ScadaSymbolObjectSettings
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { isDefinedAndNotNull, mergeDeepIgnoreArray } from '@core/utils';
import {
  ScadaSymbolBehaviorGroup,
  toBehaviorGroups
} from '@home/components/widget/lib/settings/common/scada/scada-symbol-object-settings.models';
import { Observable, of } from 'rxjs';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { ImageService } from '@core/http/image.service';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-scada-symbol-object-settings',
    templateUrl: './scada-symbol-object-settings.component.html',
    styleUrls: ['./scada-symbol-object-settings.component.scss', './../../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ScadaSymbolObjectSettingsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ScadaSymbolObjectSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ScadaSymbolObjectSettingsComponent implements OnInit, OnChanges, ControlValueAccessor, Validator {

  ScadaSymbolBehaviorType = ScadaSymbolBehaviorType;

  @Input()
  disabled: boolean;

  @Input()
  scadaSymbolUrl: string;

  @Input()
  scadaSymbolContent: string;

  @Input()
  scadaSymbolMetadata: ScadaSymbolMetadata;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  widgetType: widgetType;

  private modelValue: ScadaSymbolObjectSettings;

  private propagateChange = null;

  public scadaSymbolObjectSettingsFormGroup: UntypedFormGroup;

  metadata: ScadaSymbolMetadata;
  behaviorGroups: ScadaSymbolBehaviorGroup[];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private imageService: ImageService,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.scadaSymbolObjectSettingsFormGroup = this.fb.group({
      behavior: this.fb.group({}),
      properties: [{}, []]
    });
    this.scadaSymbolObjectSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    this.loadMetadata();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['scadaSymbolUrl', 'scadaSymbolContent', 'scadaSymbolMetadata'].includes(propName)) {
          this.loadMetadata();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.scadaSymbolObjectSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.scadaSymbolObjectSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolObjectSettings): void {
    this.modelValue = value || { behavior: {}, properties: {} };
    this.setupValue();
  }

  validate(_c: UntypedFormControl) {
    const valid = this.scadaSymbolObjectSettingsFormGroup.valid;
    return valid ? null : {
      scadaSymbolObjectSettings: {
        valid: false,
      },
    };
  }

  private loadMetadata() {
    let metadata$: Observable<ScadaSymbolMetadata>;
    if (this.scadaSymbolMetadata) {
      metadata$ = of(this.scadaSymbolMetadata);
    } else {
      let content$: Observable<string>;
      if (this.scadaSymbolContent) {
        content$ = of(this.scadaSymbolContent);
      } else if (this.scadaSymbolUrl) {
        content$ = this.imageService.getImageString(this.scadaSymbolUrl);
      } else {
        content$ = of('<svg></svg>');
      }
      metadata$ = content$.pipe(
        map(content => parseScadaSymbolMetadataFromContent(content))
      );
    }
    metadata$.subscribe(
      (metadata) => {
        this.metadata = metadata;
        this.behaviorGroups = toBehaviorGroups(this.metadata.behavior);
        const behaviorFormGroup =  this.scadaSymbolObjectSettingsFormGroup.get('behavior') as UntypedFormGroup;
        for (const control of Object.keys(behaviorFormGroup.controls)) {
          behaviorFormGroup.removeControl(control, {emitEvent: false});
        }
        for (const behavior of this.metadata.behavior) {
          behaviorFormGroup.addControl(behavior.id, this.fb.control(null, []), {emitEvent: false});
        }
        this.setupValue();
        this.cd.markForCheck();
      }
    );
  }

  private setupValue() {
    if (this.metadata) {
      const defaults = defaultScadaSymbolObjectSettings(this.metadata);
      this.modelValue = mergeDeepIgnoreArray<ScadaSymbolObjectSettings>(defaults, this.modelValue);
      this.scadaSymbolObjectSettingsFormGroup.patchValue(
        this.modelValue, {emitEvent: false}
      );
      this.setDisabledState(this.disabled);
    }
  }

  private updateModel() {
    this.modelValue = this.scadaSymbolObjectSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
