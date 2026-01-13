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

import { booleanAttribute, Directive, ElementRef, input, OnInit, Renderer2 } from '@angular/core';
import { MatTooltip, TooltipPosition } from '@angular/material/tooltip';
import { ContentObserver } from '@angular/cdk/observers';
import { merge } from 'rxjs';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

@Directive({
  selector: '[tbTruncateWithTooltip]',
  hostDirectives: [{
    directive: MatTooltip,
    inputs: ['matTooltipClass', 'matTooltipTouchGestures'],
  }]
})
export class TruncateWithTooltipDirective implements OnInit {

  text = input<string>(undefined, {alias: 'tbTruncateWithTooltip'});

  tooltipEnabled = input(true, {transform: booleanAttribute});

  position = input<TooltipPosition>('above');

  constructor(
    private elementRef: ElementRef<HTMLElement>,
    private renderer: Renderer2,
    private tooltip: MatTooltip,
    private contentObserver: ContentObserver
  ) {
    merge(toObservable(this.text), this.contentObserver.observe(this.elementRef)).pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.tooltip.message = this.text() || this.elementRef.nativeElement.innerText
    })
  }

  ngOnInit(): void {
    this.applyTruncationStyles();
    this.tooltip.position = this.position();
    this.showTooltipOnOverflow(this);
  }

  private showTooltipOnOverflow(ctx: TruncateWithTooltipDirective) {
    ctx.tooltip.show = (function(old) {
      function extendsFunction() {
        if (ctx.tooltipEnabled() && ctx.isOverflown()) {
          old.apply(ctx.tooltip, arguments);
        }
      }
      return extendsFunction;
    })(ctx.tooltip.show);
  }

  private applyTruncationStyles(): void {
    this.renderer.setStyle(this.elementRef.nativeElement, 'white-space', 'nowrap');
    this.renderer.setStyle(this.elementRef.nativeElement, 'overflow', 'hidden');
    this.renderer.setStyle(this.elementRef.nativeElement, 'text-overflow', 'ellipsis');
  }

  private isOverflown(): boolean {
    return this.elementRef.nativeElement.clientWidth < this.elementRef.nativeElement.scrollWidth;
  }
}
