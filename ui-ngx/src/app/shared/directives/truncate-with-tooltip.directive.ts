///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
} from '@angular/core';
import { fromEvent, Subject } from 'rxjs';
import { filter, takeUntil, tap } from 'rxjs/operators';
import { MatTooltip, TooltipPosition } from '@angular/material/tooltip';
import { coerceBoolean } from '@shared/decorators/coercion';

@Directive({
  selector: '[tbTruncateWithTooltip]',
  providers: [MatTooltip],
})
export class TruncateWithTooltipDirective implements OnInit, AfterViewInit, OnDestroy {

  @Input('tbTruncateWithTooltip')
  text: string;

  @Input()
  @coerceBoolean()
  tooltipEnabled = true;

  @Input()
  position: TooltipPosition = 'above';

  private destroy$ = new Subject<void>();

  constructor(
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private tooltip: MatTooltip
  ) {}

  ngOnInit(): void {
    this.observeMouseEvents();
    this.applyTruncationStyles();
  }

  ngAfterViewInit(): void {
    this.tooltip.position = this.position;
  }

  ngOnDestroy(): void {
    if (this.tooltip._isTooltipVisible()) {
      this.hideTooltip();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private observeMouseEvents(): void {
    fromEvent(this.elementRef.nativeElement, 'mouseenter')
      .pipe(
        filter(() => this.tooltipEnabled),
        filter(() => this.isOverflown(this.elementRef.nativeElement)),
        tap(() => this.showTooltip()),
        takeUntil(this.destroy$),
      )
      .subscribe();
    fromEvent(this.elementRef.nativeElement, 'mouseleave')
      .pipe(
        filter(() => this.tooltipEnabled),
        filter(() => this.tooltip._isTooltipVisible()),
        tap(() => this.hideTooltip()),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  private applyTruncationStyles(): void {
    this.renderer.setStyle(this.elementRef.nativeElement, 'white-space', 'nowrap');
    this.renderer.setStyle(this.elementRef.nativeElement, 'overflow', 'hidden');
    this.renderer.setStyle(this.elementRef.nativeElement, 'text-overflow', 'ellipsis');
  }

  private isOverflown(element: HTMLElement): boolean {
    return element.clientWidth < element.scrollWidth;
  }

  private showTooltip(): void {
    this.tooltip.message = this.text || this.elementRef.nativeElement.innerText;
    this.tooltip.show();
  }

  private hideTooltip(): void {
    this.tooltip.hide();
  }
}
