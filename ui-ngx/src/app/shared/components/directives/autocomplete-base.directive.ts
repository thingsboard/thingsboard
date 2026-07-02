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

import { Directive, ElementRef } from '@angular/core';
import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';

@Directive()
export abstract class AutocompleteBaseDirective implements ErrorStateMatcher {

  protected  isPanelOpen = false;

  protected dirty = false;

  protected searchText = '';

  protected abstract getControl(): FormControl;

  protected abstract getInput(): ElementRef<HTMLInputElement>;

  protected onTouched: () => void = () => {};

  protected propagateChange = (v: any) => { };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  protected reset(): void {
    this.getControl().patchValue('', { emitEvent: false });
  }

  clear(): void {
    this.getControl().patchValue('', { emitEvent: true });
    setTimeout(() => {
      this.getInput().nativeElement.blur();
      this.getInput().nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  onFocus(): void {
    if (this.dirty) {
      this.getControl().updateValueAndValidity({ onlySelf: true, emitEvent: true });
      this.dirty = false;
    }
  }

  onBlur(): void {
    this.onTouched();
  }


  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    if (this.isPanelOpen) {
      return false;
    }
    return !!(control?.invalid && (control.touched || form?.submitted));
  }

  onPanelOpened() {
    this.isPanelOpen = true;
  }

  onPanelClosed() {
    this.isPanelOpen = false;
    this.onTouched();
  }
}
