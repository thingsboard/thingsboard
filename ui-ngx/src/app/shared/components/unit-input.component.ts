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
import {
  AllMeasures,
  getSourceTbUnitSymbol,
  isTbUnitMapping,
  TbUnit,
  UnitInfo,
  UnitSystem
} from '@shared/models/unit.models';
import { map, mergeMap } from 'rxjs/operators';
import { UnitService } from '@core/services/unit.service';
import { TbPopoverService } from '@shared/components/popover.service';
import { UnitSettingsPanelComponent } from '@shared/components/unit-settings-panel.component';
import { isDefinedAndNotNull, isEqual, isNotEmptyStr } from '@core/utils';

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

  unitsFormControl: FormControl<TbUnit | UnitInfo>;

  @Input({transform: booleanAttribute})
  disabled: boolean;

  @Input({transform: booleanAttribute})
  required = false;

  @Input()
  tagFilter: string;

  @Input()
  measure: AllMeasures;

  @Input()
  unitSystem: UnitSystem;

  @Input({transform: booleanAttribute})
  supportsUnitConversion = false;

  @Input({transform: booleanAttribute})
  onlySystemUnits = false;

  filteredUnits$: Observable<Array<[AllMeasures, Array<UnitInfo>]>>;

  searchText = '';

  isUnitMapping = false;

  private dirty = false;

  private modelValue: TbUnit | null;

  private fetchUnits$: Observable<Array<[AllMeasures, Array<UnitInfo>]>> = null;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private unitService: UnitService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private elementRef: ElementRef) {
  }

  ngOnInit() {
    this.unitsFormControl = this.fb.control<TbUnit | UnitInfo>('', this.required ? [Validators.required] : []);
    this.filteredUnits$ = this.unitsFormControl.valueChanges.pipe(
      map(value => {
        this.updateModel(value);
        return getSourceTbUnitSymbol(value);
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
      this.unitsFormControl.patchValue(this.unitService.getUnitInfo(symbol) ?? symbol, {emitEvent: false});
      this.isUnitMapping = false;
    } else {
      this.unitsFormControl.patchValue(symbol, {emitEvent: false});
      this.isUnitMapping = isDefinedAndNotNull(symbol);
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.unitsFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayUnitFn(unit?: TbUnit | UnitInfo): string | undefined {
    if (unit) {
      return getSourceTbUnitSymbol(unit);
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
    setTimeout(() => {
      this.unitInput.nativeElement.blur();
      this.unitInput.nativeElement.focus();
    }, 0);
  }

  openUnitSettingsPopup($event: Event) {
    if (!this.supportsUnitConversion) {
      return;
    }
    $event.stopPropagation();
    this.unitInput.nativeElement.blur();
    const trigger = this.elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const popover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: UnitSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['left', 'bottom', 'top'],
        context: {
          unit: this.extractTbUnit(this.unitsFormControl.value),
          required: this.required,
          disabled: this.disabled,
          tagFilter: this.tagFilter,
          measure: this.measure
        },
        isModal: true
      });
      popover.tbComponentRef.instance.unitSettingsApplied.subscribe((unitSetting) => {
        popover.hide();
        this.unitsFormControl.patchValue(unitSetting, {emitEvent: false});
        this.updateModel(unitSetting);
      });
    }
  }

  private updateModel(value: UnitInfo | TbUnit ) {
    let res = this.extractTbUnit(value);
    if (this.onlySystemUnits && !isTbUnitMapping(res)) {
      const unitInfo = this.unitService.getUnitInfo(res as string);
      if (unitInfo) {
        if (this.measure && unitInfo.measure !== this.measure) {
          res = null;
        }
      } else {
        res = null;
      }
    }
    if (!isEqual(this.modelValue, res)) {
      this.modelValue = res;
      this.isUnitMapping = isTbUnitMapping(res);
      this.propagateChange(this.modelValue);
    }
  }

  private fetchUnits(searchText?: string): Observable<Array<[AllMeasures, Array<UnitInfo>]>> {
    this.searchText = searchText;
    return this.getGroupedUnits().pipe(
      map(unit => this.searchUnit(unit, searchText))
    );
  }

  private getGroupedUnits(): Observable<Array<[AllMeasures, Array<UnitInfo>]>> {
    if (this.fetchUnits$ === null) {
      this.fetchUnits$ = of(this.unitService.getUnitsGroupedByMeasure(this.measure, this.unitSystem)).pipe(
        map(data => {
          let objectData = Object.entries(data) as Array<[AllMeasures, UnitInfo[]]>;

          if (this.tagFilter) {
            objectData = objectData
              .map((measure) => [measure[0], measure[1].filter(u => u.tags.includes(this.tagFilter))] as [AllMeasures, UnitInfo[]])
              .filter((measure) => measure[1].length > 0);
          }
          return objectData;
        }),
        shareReplay(1)
      );
    }
    return this.fetchUnits$;
  }

  private searchUnit(units: Array<[AllMeasures, Array<UnitInfo>]>, searchText?: string): Array<[AllMeasures, Array<UnitInfo>]> {
    if (isNotEmptyStr(searchText)) {
      const filterValue = searchText.trim().toUpperCase();

      const scoredGroups = units
        .map(([measure, unitInfos]) => {
          const scoredUnits = unitInfos
            .map(unit => ({
              unit,
              score: this.calculateRelevanceScore(unit, filterValue)
            }))
            .filter(({ score }) => score > 0)
            .sort((a, b) => b.score - a.score)
            .map(({ unit }) => unit);

          let groupScore = scoredUnits.length > 0
            ? Math.max(...scoredUnits.map(unit => this.calculateRelevanceScore(unit, filterValue)))
            : 0;

          if (measure.toUpperCase() === filterValue) {
            groupScore += 200;
          }

          return { measure, units: scoredUnits, groupScore };
        })
        .filter(group => group.units.length > 0)
        .sort((a, b) => {
          if (b.groupScore !== a.groupScore) {
            return b.groupScore - a.groupScore;
          }
          return b.units.length - a.units.length;
        });

      return scoredGroups.map(group => [group.measure, group.units] as [AllMeasures, Array<UnitInfo>]);
    }
    return units;
  }

  private calculateRelevanceScore(unit: UnitInfo, filterValue: string): number {
    const name = unit.name.toUpperCase();
    const abbr = unit.abbr.toUpperCase();
    const tags = unit.tags.map(tag => tag.toUpperCase());

    let score = 0;

    if (name === filterValue || abbr === filterValue) {
      score += 100;
    } else if (tags.includes(filterValue)) {
      score += 80;
    } else if (name.startsWith(filterValue) || abbr.startsWith(filterValue)) {
      score += 60;
    } else if (tags.some(tag => tag.startsWith(filterValue))) {
      score += 50;
    } else if (tags.some(tag => tag.includes(filterValue))) {
      score += 30;
    }

    if (score > 0) {
      score += Math.max(0, 10 - (name.length + abbr.length) / 2);
    }

    return score;
  }

  private extractTbUnit(value: TbUnit | UnitInfo | null): TbUnit {
    if (value === null) {
      return null;
    }
    if (value === undefined) {
      return undefined;
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
