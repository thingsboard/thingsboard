///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, ViewChild, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'tb-widgets-bundle-search',
  templateUrl: './widgets-bundle-search.component.html',
  styleUrls: ['./widgets-bundle-search.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetsBundleSearchComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class WidgetsBundleSearchComponent implements ControlValueAccessor {

  searchText: string;

  @Input() placeholder: string;

  @ViewChild('searchInput') searchInput: ElementRef<HTMLInputElement>;

  private propagateChange = (v: any) => { };

  constructor() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(value: string | null): void {
    this.searchText = value;
  }

  updateSearchText(): void {
    this.updateView();
  }

  private updateView() {
    this.propagateChange(this.searchText);
  }

  clear(): void {
    this.searchText = '';
    this.updateView();
  }
}
