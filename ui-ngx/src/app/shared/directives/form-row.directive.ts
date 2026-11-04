// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ContentChildren, Directive, HostBinding, QueryList } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
    selector: '[tbFormRow]',
    host: {
        'class': 'tb-form-row'
    },
    standalone: false
})
export class FormRowDirective {

  @HostBinding('class.disabled')
  get disabled(): boolean {
    return !this.controls?.some(control => !control.control.disabled);
  }

  @ContentChildren(NgControl, {descendants: false})
  controls: QueryList<NgControl>;

}
