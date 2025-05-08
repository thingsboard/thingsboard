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

import { AfterViewInit, Component, forwardRef, Input, NgZone, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { getDefaultTimezoneInfo, getTimezoneInfo, getTimezones, TimezoneInfo } from '@shared/models/time/time.models';
import { deepClone } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TimeService } from '@core/services/time.service';

@Component({
  selector: 'tb-timezone-select',
  templateUrl: './timezone-select.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => TimezoneSelectComponent),
    multi: true
  }]
})
export class TimezoneSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectTimezoneFormGroup: UntypedFormGroup;

  modelValue: string | null;

  defaultTimezoneId: string = null;

  @Input()
  set defaultTimezone(timezone: string) {
    if (this.defaultTimezoneId !== timezone) {
      this.defaultTimezoneId = timezone;
    }
  }

  @Input()
  @coerceBoolean()
  displayLabel = true;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private userTimezoneByDefaultValue: boolean;
  get userTimezoneByDefault(): boolean {
    return this.userTimezoneByDefaultValue;
  }
  @Input()
  set userTimezoneByDefault(value: boolean) {
    this.userTimezoneByDefaultValue = coerceBooleanProperty(value);
  }

  private localBrowserTimezonePlaceholderOnEmptyValue: boolean;
  get localBrowserTimezonePlaceholderOnEmpty(): boolean {
    return this.localBrowserTimezonePlaceholderOnEmptyValue;
  }
  @Input()
  set localBrowserTimezonePlaceholderOnEmpty(value: boolean) {
    this.localBrowserTimezonePlaceholderOnEmptyValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @ViewChild('timezoneInput', {static: true, read: MatAutocompleteTrigger}) timezoneInputTrigger: MatAutocompleteTrigger;

  filteredTimezones: Observable<Array<TimezoneInfo>>;

  searchText = '';

  ignoreClosePanel = false;

  private dirty = false;

  private localBrowserTimezoneInfoPlaceholder: TimezoneInfo = this.timeService.getLocalBrowserTimezoneInfoPlaceholder();

  private timezones: Array<TimezoneInfo>;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private ngZone: NgZone,
              private fb: UntypedFormBuilder,
              private timeService: TimeService) {
    this.selectTimezoneFormGroup = this.fb.group({
      timezone: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredTimezones = this.selectTimezoneFormGroup.get('timezone').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchTimezones(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectTimezoneFormGroup.disable({emitEvent: false});
    } else {
      this.selectTimezoneFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    const foundTimezone = getTimezoneInfo(value, this.defaultTimezoneId, this.userTimezoneByDefaultValue);
    if (foundTimezone !== null) {
      this.selectTimezoneFormGroup.get('timezone').patchValue(foundTimezone, {emitEvent: false});
      if (foundTimezone.id !== value) {
        setTimeout(() => {
          this.updateView(foundTimezone.id);
        }, 0);
      } else {
        this.modelValue = value;
      }
    } else {
      this.modelValue = null;
      if (this.localBrowserTimezonePlaceholderOnEmptyValue) {
        this.selectTimezoneFormGroup.get('timezone').patchValue(this.localBrowserTimezoneInfoPlaceholder, {emitEvent: false});
      } else {
        this.selectTimezoneFormGroup.get('timezone').patchValue('', {emitEvent: false});
      }
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectTimezoneFormGroup.get('timezone').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  onPanelClosed() {
    if (this.ignoreClosePanel) {
      this.ignoreClosePanel = false;
    } else {
      if (!this.modelValue) {
        if (this.defaultTimezoneId || this.userTimezoneByDefaultValue) {
          const defaultTimezoneInfo = getTimezoneInfo(this.defaultTimezoneId, this.defaultTimezoneId, this.userTimezoneByDefaultValue);
          if (defaultTimezoneInfo !== null) {
            this.ngZone.run(() => {
              this.selectTimezoneFormGroup.get('timezone').reset(defaultTimezoneInfo, {emitEvent: true});
            });
          }
        } else if (this.localBrowserTimezonePlaceholderOnEmptyValue) {
          this.ngZone.run(() => {
            this.selectTimezoneFormGroup.get('timezone').reset(this.localBrowserTimezoneInfoPlaceholder, {emitEvent: true});
          });
        }
      }
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayTimezoneFn(timezone?: TimezoneInfo): string | undefined {
    return timezone ? `${timezone.name} (${timezone.offset})` : undefined;
  }

  fetchTimezones(searchText?: string): Observable<Array<TimezoneInfo>> {
    this.searchText = searchText;
    if (searchText && searchText.length) {
      return of(this.loadTimezones().filter((timezoneInfo) =>
          timezoneInfo.name.toLowerCase().includes(searchText.toLowerCase())));
    }
    return of(this.loadTimezones());
  }

  clear() {
    this.selectTimezoneFormGroup.get('timezone').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.timezoneInputTrigger.openPanel();
    }, 0);
  }

  private loadTimezones(): Array<TimezoneInfo> {
    if (!this.timezones) {
      this.timezones = [];
      if (this.localBrowserTimezonePlaceholderOnEmptyValue) {
        this.timezones.push(this.localBrowserTimezoneInfoPlaceholder);
      }
      this.timezones.push(...getTimezones());
    }
    return this.timezones;
  }

}
