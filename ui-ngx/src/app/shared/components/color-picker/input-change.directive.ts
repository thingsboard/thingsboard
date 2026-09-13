// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
