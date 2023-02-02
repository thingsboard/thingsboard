///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { TranslateService } from '@ngx-translate/core';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { Observable } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { FloatLabelType } from '@angular/material/form-field/form-field';

@Component({
  selector: 'tb-alarm-severities-list',
  templateUrl: './alarm-severities-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmSeveritiesListComponent),
      multi: true
    }
  ]
})
export class AlarmSeveritiesListComponent implements ControlValueAccessor{

  @ViewChild('severityInput', {static: true}) severityInput: ElementRef<HTMLInputElement>;
  @ViewChild('severityInput', {read: MatAutocompleteTrigger, static: true}) autocompleteTrigger: MatAutocompleteTrigger;

  alarmSeveritiesForm: FormGroup;

  alarmSeverityTranslationMap = alarmSeverityTranslations;
  private alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverity = AlarmSeverity;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  private modelValue: Array<string> | null;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  @Input()
  flotLabel: FloatLabelType = 'auto';

  searchText = '';
  filteredDisplaySeverities: Observable<Array<string>>;

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              public truncate: TruncatePipe) {
    this.alarmSeveritiesForm = this.fb.group({
      alarmSeverities: [null, this.required ? [Validators.required] : []],
      alarmSeverity: ['']
    });

    this.filteredDisplaySeverities = this.alarmSeveritiesForm.get('alarmSeverity').valueChanges
      .pipe(
        map((value) => value ? value : ''),
        map(name => this.fetchSeverities(name) ),
        share()
      );
  }

  updateValidators() {
    this.alarmSeveritiesForm.get('alarmSeverities').setValidators(this.required ? [Validators.required] : []);
    this.alarmSeveritiesForm.get('alarmSeverities').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.alarmSeveritiesForm.disable({emitEvent: false});
    } else {
      this.alarmSeveritiesForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.alarmSeveritiesForm.get('alarmSeverities').setValue(value);
    } else {
      this.alarmSeveritiesForm.get('alarmSeverities').setValue(null);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  addSeverityFromAutocomplete(event: MatAutocompleteSelectedEvent) {
    this.addSeverity(event.option.value);
  }

  addSeverityFromChipInput(event: MatChipInputEvent): void {
    const alarmSeverity = event.value || '';
    const result = this.fetchSeverities(alarmSeverity.trim());
    if (result.length === 1) {
      this.addSeverity(result[0]);
    }
  }

  private addSeverity(alarmSeverity: string) {
    if (alarmSeverity) {
      if (!this.modelValue || this.modelValue.indexOf(alarmSeverity) === -1) {
        if (!this.modelValue) {
          this.modelValue = [];
        }
        this.modelValue.push(alarmSeverity);
        this.alarmSeveritiesForm.get('alarmSeverities').setValue(this.modelValue);
      }
      this.propagateChange(this.modelValue);
      if (!this.fetchSeverities().length) {
        this.clear('', true);
        this.autocompleteTrigger.closePanel();
      } else {
        this.clear();
      }
    }
  }

  onSeverityRemoved(alarmSeverity: string) {
    const index = this.modelValue.indexOf(alarmSeverity);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.alarmSeveritiesForm.get('alarmSeverities').setValue(this.modelValue);
      this.propagateChange(this.modelValue);
      this.clear(this.severityInput.nativeElement.value, true);
      this.autocompleteTrigger.closePanel();
    }
  }

  displaySeverityFn(severity?: string): string | undefined {
    return severity ? this.translate.instant(alarmSeverityTranslations.get(AlarmSeverity[severity])) : undefined;
  }

  onFocus() {
    if (this.dirty) {
      this.alarmSeveritiesForm.get('alarmSeverity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  private fetchSeverities(searchText?: string): Array<string> {
    this.searchText = searchText;
    const allowAlarmSeverity = this.alarmSeverities.filter(severity => !this.modelValue?.includes(severity));
    if (this.textIsNotEmpty(this.searchText)) {
      const search = this.searchText.toUpperCase();
      return allowAlarmSeverity.filter(severity => severity.toUpperCase().includes(search));
    } else {
      return allowAlarmSeverity;
    }
  }

  private clear(value: string = '', ignoreFocus = false) {
    this.severityInput.nativeElement.value = value;
    this.alarmSeveritiesForm.get('alarmSeverity').patchValue(value);
    if (!ignoreFocus) {
      setTimeout(() => {
        this.severityInput.nativeElement.blur();
        this.severityInput.nativeElement.focus();
      }, 0);
    }
  }

}
