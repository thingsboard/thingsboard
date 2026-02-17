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
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Inject,
  InjectionToken,
  Input,
  OnInit,
  Optional,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, isArraysEqualIgnoreUndefined, isDefinedAndNotNull, isEmpty, isUndefinedOrNull } from '@core/utils';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { UtilsService } from '@core/services/utils.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { alarmRuleEntityTypeList, AlarmRuleFilterConfig } from "@shared/models/alarm-rule.models";

export const ALARM_FILTER_CONFIG_DATA = new InjectionToken<any>('AlarmRuleFilterConfigData');

export interface AlarmRuleFilterConfigData {
  panelMode: boolean;
  userMode: boolean;
  alarmRuleFilterConfig: AlarmRuleFilterConfig;
  initialAlarmRuleFilterConfig?: AlarmRuleFilterConfig;
}

@Component({
    selector: 'tb-alarm-rule-filter-config',
    templateUrl: './alarm-rule-filter-config.component.html',
    styleUrls: ['./alarm-rule-filter-config.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AlarmRuleFilterConfigComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AlarmRuleFilterConfigComponent implements OnInit, ControlValueAccessor {

  @ViewChild('alarmRuleFilterPanel')
  alarmRuleFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @Input()
  initialAlarmRuleFilterConfig: AlarmRuleFilterConfig = {
    name: [],
    entityType: null,
    entities: []
  };

  panelMode = false;

  buttonDisplayValue = this.translate.instant('alarm-rule.alarm-rule-filter-title');

  alarmRuleFilterConfigForm: FormGroup;

  alarmFilterOverlayRef: OverlayRef;

  panelResult: AlarmRuleFilterConfig = null;

  entityType = EntityType;

  listEntityTypes = alarmRuleEntityTypeList;
  entityTypeTranslations = entityTypeTranslations;

  private alarmRuleFilterConfig: AlarmRuleFilterConfig;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(ALARM_FILTER_CONFIG_DATA)
              private data: AlarmRuleFilterConfigData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: FormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.alarmRuleFilterConfig = this.data.alarmRuleFilterConfig;
      this.initialAlarmRuleFilterConfig = this.data.initialAlarmRuleFilterConfig;
      if (this.panelMode && !this.initialAlarmRuleFilterConfig) {
        this.initialAlarmRuleFilterConfig = deepClone(this.alarmRuleFilterConfig);
      }
    }
    this.alarmRuleFilterConfigForm = this.fb.group({
      name: [null, []],
      entityType: [null, []],
      entities: [null, []]
    });
    this.alarmRuleFilterConfigForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.alarmRuleConfigUpdated(this.alarmRuleFilterConfigForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateAlarmRuleConfigForm(this.alarmRuleFilterConfig);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmRuleFilterConfigForm.disable({emitEvent: false});
    } else {
      this.alarmRuleFilterConfigForm.enable({emitEvent: false});
    }
  }

  writeValue(alarmRuleFilterConfig?: AlarmRuleFilterConfig): void {
    this.alarmRuleFilterConfig = alarmRuleFilterConfig;
    if (!this.initialAlarmRuleFilterConfig && alarmRuleFilterConfig) {
      this.initialAlarmRuleFilterConfig = deepClone(alarmRuleFilterConfig);
    }
    this.updateButtonDisplayValue();
    this.updateAlarmRuleConfigForm(alarmRuleFilterConfig);
  }

  toggleAlarmRuleFilterPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    config.positionStrategy = this.overlay.position()
      .flexibleConnectedTo(this.nativeElement)
      .withPositions([POSITION_MAP.bottomLeft]);

    this.alarmFilterOverlayRef = this.overlay.create(config);
    this.alarmFilterOverlayRef.backdropClick().subscribe(() => {
      this.alarmFilterOverlayRef.dispose();
    });
    this.alarmFilterOverlayRef.attach(new TemplatePortal(this.alarmRuleFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.alarmFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateAlarmRuleConfigForm(this.alarmRuleFilterConfig);
    this.alarmRuleFilterConfigForm.markAsPristine();
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.alarmFilterOverlayRef.dispose();
    }
  }

  update() {
    this.alarmRuleConfigUpdated(this.alarmRuleFilterConfigForm.value);
    this.alarmRuleFilterConfigForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.alarmRuleFilterConfig;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.alarmFilterOverlayRef.dispose();
    }
  }

  reset() {
    const alarmRuleFilterConfig = this.alarmRuleFilterConfigFromFormValue(this.alarmRuleFilterConfigForm.value);
    if (!this.alarmRuleFilterConfigEquals(alarmRuleFilterConfig, this.initialAlarmRuleFilterConfig)) {
      this.updateAlarmRuleConfigForm(this.initialAlarmRuleFilterConfig);
      this.alarmRuleFilterConfigForm.markAsDirty();
    }
  }

  private alarmRuleFilterConfigEquals = (filter1?: AlarmRuleFilterConfig, filter2?: AlarmRuleFilterConfig): boolean => {
    if (filter1 === filter2) {
      return true;
    }
    if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
      return true;
    } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
      if (!isArraysEqualIgnoreUndefined(filter1.name, filter2.name)) {
        return false;
      }
      if (!isArraysEqualIgnoreUndefined(filter1.entities, filter2.entities)) {
        return false;
      }
      return filter1.entityType === filter2.entityType;
    }
    return false;
  };

  private updateAlarmRuleConfigForm(alarmRuleFilterConfig?: AlarmRuleFilterConfig) {
    this.alarmRuleFilterConfigForm.patchValue({
      name: alarmRuleFilterConfig?.name ?? [],
      entityType: alarmRuleFilterConfig?.entityType ?? null,
      entities: alarmRuleFilterConfig?.entities ?? [],
    }, {emitEvent: false});
  }

  private alarmRuleConfigUpdated(formValue: any) {
    this.alarmRuleFilterConfig = this.alarmRuleFilterConfigFromFormValue(formValue);
    this.updateButtonDisplayValue();
    this.propagateChange(this.alarmRuleFilterConfig);
  }

  private alarmRuleFilterConfigFromFormValue(formValue: any): AlarmRuleFilterConfig {
    return {
      name: formValue?.name ?? [],
      entityType: formValue?.entityType ?? null,
      entities: formValue?.entities ?? [],
    };
  }

  private updateButtonDisplayValue() {
    if (this.buttonMode) {
      const filterTextParts: string[] = [];
      if (this.alarmRuleFilterConfig?.name?.length) {
        filterTextParts.push(this.alarmRuleFilterConfig.name.map((type) => this.customTranslate(type)).join(', '));
      }
      if (this.alarmRuleFilterConfig?.entityType) {
        filterTextParts.push(this.translate.instant( entityTypeTranslations.get(this.alarmRuleFilterConfig.entityType).type));
      }
      if (!filterTextParts.length) {
        this.buttonDisplayValue = this.translate.instant('alarm-rule.alarm-rule-filter-title');
      } else {
        this.buttonDisplayValue = this.translate.instant('alarm-rule.filter-title') + `: ${filterTextParts.join(', ')}`;
      }
    }
  }

  private customTranslate(entity: string) {
    return this.utils.customTranslation(entity, entity);
  }

}
