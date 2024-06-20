///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  ElementRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Observable, of, shareReplay, switchMap } from 'rxjs';
import { getUnits, searchUnits, Unit, unitBySymbol, UnitsType } from '@shared/models/unit.models';
import { map, mergeMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ResourcesService } from '@core/services/resources.service';
import { coerceBoolean } from '@shared/decorators/coercion';

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
export class UnitInputComponent implements ControlValueAccessor, OnInit {

  @HostBinding('style.display') get hostDisplay() {return 'flex';};

  unitsFormControl: FormControl;

  modelValue: string | null;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  tagFilter: UnitsType;

  @ViewChild('unitInput', {static: true}) unitInput: ElementRef;

  filteredUnits: Observable<Array<Unit | string>>;

  searchText = '';

  private dirty = false;

  private fetchUnits$: Observable<Array<Unit>> = null;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private resourcesService: ResourcesService,
              private translate: TranslateService) {
  }

  ngOnInit() {
    this.unitsFormControl = this.fb.control('', this.required ? [Validators.required] : []);
    this.filteredUnits = this.unitsFormControl.valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        map(value => (value as Unit)?.symbol ? (value as Unit).symbol : (value ? value as string : '')),
        mergeMap(symbol => this.fetchUnits(symbol))
      );
  }

  writeValue(symbol?: string): void {
    this.searchText = '';
    this.modelValue = symbol;
    of(symbol).pipe(
      switchMap(value => value
        ? this.unitsConstant().pipe(map(units => unitBySymbol(units, value) ?? value))
        : of(null))
    ).subscribe(result => {
      this.unitsFormControl.patchValue(result, {emitEvent: false});
      this.dirty = true;
    });
  }

  onFocus() {
    if (this.dirty) {
      this.unitsFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: Unit | string | null) {
    const res: string = (value as Unit)?.symbol ? (value as Unit)?.symbol : (value as string);
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.propagateChange(this.modelValue);
    }
  }

  displayUnitFn(unit?: Unit | string): string | undefined {
    if (unit) {
      if ((unit as Unit).symbol) {
        return (unit as Unit).symbol;
      } else {
        return unit as string;
      }
    }
    return undefined;
  }

  fetchUnits(searchText?: string): Observable<Array<Unit | string>> {
    this.searchText = searchText;
    return this.unitsConstant().pipe(
      map(unit => searchUnits(unit, searchText))
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.unitsFormControl.disable({emitEvent: false});
    } else {
      this.unitsFormControl.enable({emitEvent: false});
    }
  }

  clear() {
    this.unitsFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.unitInput.nativeElement.blur();
      this.unitInput.nativeElement.focus();
    }, 0);
  }

  private unitsConstant(): Observable<Array<Unit>> {
    if (this.fetchUnits$ === null) {
      this.fetchUnits$ = getUnits(this.resourcesService).pipe(
        map((units) => {
          if (this.tagFilter) {
            units = units.filter(u => u.tags.includes(this.tagFilter));
          }
          return units.map(u => ({
            symbol: u.symbol,
            name: this.translate.instant(u.name),
            tags: u.tags
          }));
        }),
        shareReplay(1)
      );
    }
    return this.fetchUnits$;
  }
}
