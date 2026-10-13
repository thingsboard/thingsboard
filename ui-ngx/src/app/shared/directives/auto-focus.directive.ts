// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Directive, ElementRef } from '@angular/core';

@Directive({
  selector: '[tb-auto-focus]',
  standalone: false
})
export class AutofocusDirective implements AfterViewInit {
  constructor(private el: ElementRef<HTMLInputElement>) {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.el.nativeElement.focus();
      this.el.nativeElement.select();
    });
  }
}
