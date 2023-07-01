///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  AfterContentInit,
  ChangeDetectorRef,
  Component,
  ContentChildren,
  Directive,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  QueryList
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject } from 'rxjs';
import { startWith, takeUntil } from 'rxjs/operators';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';

@Directive(
  {
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tb-toggle-option',
  }
)
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ToggleSelectOption {

  @Input() value: any;

  get viewValue(): string {
    return (this._element?.nativeElement.textContent || '').trim();
  }

  constructor(
    private _element: ElementRef<HTMLElement>
  ) {}
}

@Component({
  selector: 'tb-toggle-select',
  templateUrl: './toggle-select.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ToggleSelectComponent),
      multi: true
    }
  ]
})
export class ToggleSelectComponent extends PageComponent implements AfterContentInit, OnDestroy, ControlValueAccessor {

  @ContentChildren(ToggleSelectOption) toggleSelectOptions: QueryList<ToggleSelectOption>;

  @Input()
  disabled: boolean;

  options: ToggleHeaderOption[] = [];

  private _destroyed = new Subject<void>();

  modelValue: any;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngAfterContentInit(): void {
    this.toggleSelectOptions.changes.pipe(startWith(null), takeUntil(this._destroyed)).subscribe(() => {
      this.syncToggleHeaderOptions();
    });
  }

  ngOnDestroy() {
    this._destroyed.next();
    this._destroyed.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    this.modelValue = value;
  }

  private syncToggleHeaderOptions() {
    this.options.length = 0;
    if (this.toggleSelectOptions) {
      this.toggleSelectOptions.forEach(selectOption => {
        this.options.push(
          { name: selectOption.viewValue,
            value: selectOption.value
          }
        );
      });
    }
  }

  updateModel(value: any) {
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
