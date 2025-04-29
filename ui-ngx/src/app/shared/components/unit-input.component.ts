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
  booleanAttribute,
  Component,
  ElementRef,
  forwardRef,
  HostBinding,
  Input,
  OnChanges,
  OnInit,
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, shareReplay } from 'rxjs';
import { searchUnits, TbUnit, UnitDescription, UnitsType, UnitSystem } from '@shared/models/unit.models';
import { map, mergeMap } from 'rxjs/operators';
import { AllMeasures } from '@core/services/unit/definitions/all';
import { UnitService } from '@core/services/unit/unit.service';
import { TbPopoverService } from '@shared/components/popover.service';
import { ConvertUnitSettingsPanelComponent } from '@shared/components/convert-unit-settings-panel.component';
import { isNotEmptyStr, isObject } from '@core/utils';

@Component({
  selector: 'tb-unit-input',
  templateUrl: './unit-input.component.html',
  styleUrls: ['./unit-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UnitInputComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class UnitInputComponent implements ControlValueAccessor, OnInit, OnChanges {

  @HostBinding('style.display') readonly hostDisplay = 'flex';
  @ViewChild('unitInput', {static: true}) unitInput: ElementRef;

  unitsFormControl: FormControl<TbUnit | UnitDescription>;

  @Input({transform: booleanAttribute})
  disabled: boolean;

  @Input({transform: booleanAttribute})
  required = false;

  @Input()
  tagFilter: UnitsType;

  @Input()
  measure: AllMeasures;

  @Input()
  unitSystem: UnitSystem;

  @Input({transform: booleanAttribute})
  allowConverted = false;

  filteredUnits: Observable<Array<[AllMeasures, Array<UnitDescription>]>>;

  searchText = '';

  isUnitMapping = false;

  private dirty = false;

  private modelValue: TbUnit | null;

  private fetchUnits$: Observable<Array<[AllMeasures, Array<UnitDescription>]>> = null;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private unitService: UnitService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private elementRef: ElementRef) {
  }

  ngOnInit() {
    this.unitsFormControl = this.fb.control<TbUnit | UnitDescription>('', this.required ? [Validators.required] : []);
    this.filteredUnits = this.unitsFormControl.valueChanges
      .pipe(
        map(value => {
          this.updateView(value);
          return this.getUnitSymbol(value);
        }),
        mergeMap(symbol => this.fetchUnits(symbol))
      );
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'measure' || propName === 'unitSystem') {
          this.fetchUnits$ = null;
          this.dirty = true;
        }
      }
    }
  }

  writeValue(symbol?: TbUnit): void {
    this.searchText = '';
    this.modelValue = symbol;
    if (typeof symbol === 'string') {
      this.unitsFormControl.patchValue(this.unitService.getUnitDescription(symbol) ?? symbol, {emitEvent: false});
      this.isUnitMapping = false;
    } else {
      this.unitsFormControl.patchValue(symbol, {emitEvent: false});
      this.isUnitMapping = symbol !== null;
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.unitsFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayUnitFn(unit?: TbUnit | UnitDescription): string | undefined {
    if (unit) {
      return this.getUnitSymbol(unit);
    }
    return undefined;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.unitsFormControl.disable({emitEvent: false});
    } else {
      this.unitsFormControl.enable({emitEvent: false});
    }
  }

  clear($event: Event) {
    $event.stopPropagation();
    this.unitsFormControl.patchValue(null, {emitEvent: true});
    if (!this.allowConverted) {
      setTimeout(() => {
        this.unitInput.nativeElement.blur();
        this.unitInput.nativeElement.focus();
      }, 0);
    }
  }

  openConvertSettingsPopup($event: Event) {
    if (!this.allowConverted) {
      return;
    }
    if ($event) {
      $event.stopPropagation();
    }
    this.unitInput.nativeElement.blur();
    const trigger = this.elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const convertUnitSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ConvertUnitSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['left', 'bottom', 'top'],
        context: {
          unit: this.getTbUnit(this.unitsFormControl.value),
          required: this.required,
          disabled: this.disabled,
        },
        isModal: true
      });
      convertUnitSettingsPanelPopover.tbComponentRef.instance.unitSettingsApplied.subscribe((unitSetting) => {
        convertUnitSettingsPanelPopover.hide();
        this.unitsFormControl.patchValue(unitSetting, {emitEvent: false});
        this.updateView(unitSetting);
      });
    }
  }

  private updateView(value: UnitDescription | TbUnit ) {
    const res = this.getTbUnit(value);
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.isUnitMapping = (res !== null && isObject(res));
      this.propagateChange(this.modelValue);
    }
  }

  private fetchUnits(searchText?: string): Observable<Array<[AllMeasures, Array<UnitDescription>]>> {
    this.searchText = searchText;
    return this.unitsConstant().pipe(
      map(unit => this.searchUnit(unit, searchText))
    );
  }

  private unitsConstant(): Observable<Array<[AllMeasures, Array<UnitDescription>]>> {
    if (this.fetchUnits$ === null) {
      this.fetchUnits$ = of(this.unitService.getUnitsGroupByMeasure(this.measure, this.unitSystem)).pipe(
        map(data => {
          let objectData = Object.entries(data) as Array<[AllMeasures, UnitDescription[]]>;

          if (this.tagFilter) {
            objectData = objectData
              .map((measure) => [measure[0], measure[1].filter(u => u.tags.includes(this.tagFilter))] as [AllMeasures, UnitDescription[]])
              .filter((measure) => measure[1].length > 0);
          }
          return objectData;
        }),
        shareReplay(1)
      );
    }
    return this.fetchUnits$;
  }

  private searchUnit(units: Array<[AllMeasures, Array<UnitDescription>]>, searchText?: string): Array<[AllMeasures, Array<UnitDescription>]> {
    if (isNotEmptyStr(searchText)) {
      const filterValue = searchText.trim().toUpperCase()
      return units
        .map(measure => [measure[0], searchUnits(measure[1], filterValue)] as [AllMeasures, UnitDescription[]])
        .filter((measure) => measure[1].length > 0);
    }
    return units;
  }

  private getUnitSymbol(value: TbUnit | UnitDescription | null): string {
    if (value === null) {
      return '';
    }
    if (typeof value === 'string') {
      return value;
    }
    if ('abbr' in value) {
      return value.abbr;
    }
    return value.from;
  }

  private getTbUnit(value: TbUnit | UnitDescription | null): TbUnit {
    if (value === null) {
      return null;
    }
    if (typeof value === 'string') {
      return value;
    }
    if ('abbr' in value) {
      return value.abbr;
    }
    return value;
  }
}
