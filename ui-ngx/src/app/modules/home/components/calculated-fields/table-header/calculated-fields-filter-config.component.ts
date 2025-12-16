///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { deepClone, isArraysEqualIgnoreUndefined, isDefinedAndNotNull, isEmpty, isUndefinedOrNull } from '@core/utils';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { fromEvent, Subscription } from 'rxjs';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  calculatedFieldsEntityTypeList,
  CalculatedFieldsQuery,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations
} from '@shared/models/calculated-field.models';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { TranslateService } from '@ngx-translate/core';

export const CALCULATED_FIELDS_CONFIG_DATA = new InjectionToken<any>('CalculatedFieldsFilterConfigData');

export interface CalculatedFieldsFilterConfigData {
  panelMode: boolean;
  userMode: boolean;
  filterConfig: CalculatedFieldsQuery;
  initialFilterConfig?: CalculatedFieldsQuery;
}

@Component({
  selector: 'tb-calculated-fields-filter-config',
  templateUrl: './calculated-fields-filter-config.component.html',
  styleUrls: ['./calculated-fields-filter-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldsFilterConfigComponent),
      multi: true
    }
  ]
})
export class CalculatedFieldsFilterConfigComponent implements OnInit, ControlValueAccessor {

  @ViewChild('calculatedFieldsFilterPanel')
  calculatedFieldsFilterPanel: TemplateRef<any>;

  @Input()
  disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @coerceBoolean()
  @Input()
  userMode = false;

  @coerceBoolean()
  @Input()
  propagatedFilter = true;

  @Input()
  initialCfFilterConfig: CalculatedFieldsQuery = {
    name: [],
    types: [],
    entityType: null,
    entities: []
  };

  panelMode = false;

  cfFilterForm: FormGroup;

  panelResult: CalculatedFieldsQuery = null;

  entityType = EntityType;

  listEntityTypes = calculatedFieldsEntityTypeList;
  entityTypeTranslations = entityTypeTranslations;

  readonly types: StringItemsOption[] = calculatedFieldTypes.map(item => ({
    name: this.translate.instant(CalculatedFieldTypeTranslations.get(item).name),
    value: item
  }));

  private cfFilterOverlayRef: OverlayRef;
  private cfFilterConfig: CalculatedFieldsQuery;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(CALCULATED_FIELDS_CONFIG_DATA)
              private data: CalculatedFieldsFilterConfigData | undefined,
              @Optional() private overlayRef: OverlayRef,
              private fb: FormBuilder,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private translate: TranslateService) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.userMode = this.data.userMode;
      this.cfFilterConfig = this.data.filterConfig;
      this.initialCfFilterConfig = this.data.initialFilterConfig;
      if (this.panelMode && !this.initialCfFilterConfig) {
        this.initialCfFilterConfig = deepClone(this.cfFilterConfig);
      }
    }
    this.cfFilterForm = this.fb.group({
      name: [null, []],
      types: [null, []],
      entityType: [null, []],
      entities: [null, []]
    });
    this.cfFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.cfConfigUpdated(this.cfFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateCfConfigForm(this.cfFilterConfig);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.cfFilterForm.disable({emitEvent: false});
    } else {
      this.cfFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(cfFilterConfig?: CalculatedFieldsQuery): void {
    this.cfFilterConfig = cfFilterConfig;
    if (!this.initialCfFilterConfig && cfFilterConfig) {
      this.initialCfFilterConfig = deepClone(cfFilterConfig);
    }
    this.updateCfConfigForm(cfFilterConfig);
  }

  toggleCfFilterPanel($event: Event) {
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

    this.cfFilterOverlayRef = this.overlay.create(config);
    this.cfFilterOverlayRef.backdropClick().subscribe(() => {
      this.cfFilterOverlayRef.dispose();
    });
    this.cfFilterOverlayRef.attach(new TemplatePortal(this.calculatedFieldsFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.cfFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateCfConfigForm(this.cfFilterConfig);
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.cfFilterOverlayRef.dispose();
    }
  }

  update() {
    this.cfConfigUpdated(this.cfFilterForm.value);
    this.cfFilterForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.cfFilterConfig;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.cfFilterOverlayRef.dispose();
    }
  }

  reset() {
    const cfFilterConfig = this.cfFilterFromFormValue(this.cfFilterForm.value);
    if (!this.cfFilterConfigEquals(cfFilterConfig, this.initialCfFilterConfig)) {
      this.updateCfConfigForm(this.initialCfFilterConfig);
      this.cfFilterForm.markAsDirty();
    }
  }

  private cfFilterConfigEquals = (filter1?: CalculatedFieldsQuery, filter2?: CalculatedFieldsQuery): boolean => {
    if (filter1 === filter2) {
      return true;
    }
    if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
      return true;
    } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
      if (!isArraysEqualIgnoreUndefined(filter1.name, filter2.name)) {
        return false;
      }
      if (!isArraysEqualIgnoreUndefined(filter1.types, filter2.types)) {
        return false;
      }
      if (!isArraysEqualIgnoreUndefined(filter1.entities, filter2.entities)) {
        return false;
      }
      return filter1.entityType !== filter2.entityType;
    }
    return false;
  };

  private updateCfConfigForm(cfFilterConfig?: CalculatedFieldsQuery) {
    this.cfFilterForm.patchValue({
      name: cfFilterConfig?.name ?? [],
      types: cfFilterConfig?.types ?? [],
      entityType: cfFilterConfig?.entityType ?? null,
      entities: cfFilterConfig?.entities ?? [],
    }, {emitEvent: false});
  }

  private cfConfigUpdated(formValue: any) {
    this.cfFilterConfig = this.cfFilterFromFormValue(formValue);
    this.propagateChange(this.cfFilterConfig);
  }

  private cfFilterFromFormValue(formValue: any): CalculatedFieldsQuery {
    return {
      name: formValue?.name ?? [],
      types: formValue?.types ?? [],
      entityType: formValue?.entityType ?? null,
      entities: formValue?.entities ?? [],
    };
  }
}
