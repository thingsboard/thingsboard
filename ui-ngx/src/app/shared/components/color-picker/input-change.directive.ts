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

import { Directive, EventEmitter, HostBinding, HostListener, Input, numberAttribute, Output } from '@angular/core';

@Directive({
  selector: '[inputChange]',
  standalone: false
})
export class InputChangeDirective {

  @Input({transform: numberAttribute})
  @HostBinding('attr.min')
  min = 0;

  @Input({transform: numberAttribute})
  @HostBinding('attr.max')
  max = 255;

  @Output()
  public inputChange = new EventEmitter<number>();

  @HostListener('input', ['$event'])
  public inputChanges(event: any): void {
    const element = event.target as HTMLInputElement || event.srcElement as HTMLInputElement;
    const value = element.value;

    const numeric = parseFloat(value);
    if (!isNaN(numeric) && numeric >= this.min && numeric <= this.max) {
      this.inputChange.emit(numeric);
    }
  }
}
