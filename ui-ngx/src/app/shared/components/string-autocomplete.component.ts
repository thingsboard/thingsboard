///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {
  Component,
  Input,
  forwardRef,
  OnInit,
  AfterViewInit,
  HostBinding,
  ViewChild,
  ElementRef,
  Injector
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  FormControl,
  Validators,
  FormBuilder
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { tap, map, switchMap } from 'rxjs/operators';

@Component({
  selector: 'tb-string-autocomplete',
  templateUrl: './string-autocomplete.component.html',
  styleUrls: ['./string-autocomplete.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringAutocompleteComponent),
      multi: true
    }
  ]
})
export class StringAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  @HostBinding('style.display') get hostDisplay() {return 'flex';};

  @Input()
  disabled: boolean;

  @Input()
  required: boolean;

  @Input() fetchOptionsFn: (searchText?: string) => Observable<Array<string>>;

  @ViewChild('nameInput', {static: true}) nameInput: ElementRef;

  selectionFormControl: FormControl;

  modelValue: string | null;

  filteredOptions: Observable<Array<string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_val: any) => {};

  constructor(private injector: Injector,
              private fb: FormBuilder) {
  }

  ngOnInit() {
    this.selectionFormControl = this.fb.control('', []);
    this.filteredOptions = this.selectionFormControl.valueChanges
      .pipe(
        tap(value => this.updateView(value)),
        map(value => value ? value : ''),
        switchMap(value => this.fetchOptionsFn ? this.fetchOptionsFn(value) : of([]))
      );
  }

  ngAfterViewInit() {
    if (this.required) {
      this.selectionFormControl.setValidators([Validators.required]);
    }
  }

  writeValue(option?: string): void {
    this.searchText = '';
    this.modelValue = option ? option : null;

    if (this.fetchOptionsFn) {
      this.fetchOptionsFn(option)
        .pipe(
          map(options => {
            if (options) {
              const foundOption = options.find(opt => opt === option);
              return foundOption ? foundOption : option;
            }

            return option;
          })
        )
        .subscribe(result => {
          this.selectionFormControl.patchValue(result, { emitEvent: false });
          this.dirty = true;
        });
    } else {
      this.selectionFormControl.patchValue(null, { emitEvent: false });
      this.dirty = true;
    }
  }

  onFocus() {
    if (this.dirty) {
      this.selectionFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string) {
    this.searchText = value ? value : '';
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
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
      this.selectionFormControl.disable({emitEvent: false});
    } else {
      this.selectionFormControl.enable({emitEvent: false});
    }
  }

  clear() {
    this.selectionFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.nameInput.nativeElement.blur();
      this.nameInput.nativeElement.focus();
    }, 0);
  }
}
