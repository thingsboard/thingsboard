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

import { AfterViewInit, Directive, ElementRef, Input, OnInit, Renderer2 } from '@angular/core';
import { MatTooltip, TooltipPosition } from '@angular/material/tooltip';
import { coerceBoolean } from '@shared/decorators/coercion';

@Directive({
  selector: '[tbTruncateWithTooltip]',
  hostDirectives: [{
    directive: MatTooltip,
    inputs: ['matTooltipClass', 'matTooltipTouchGestures'],
  }]
})
export class TruncateWithTooltipDirective implements OnInit, AfterViewInit {

  @Input('tbTruncateWithTooltip')
  text: string;

  @Input()
  @coerceBoolean()
  tooltipEnabled = true;

  @Input()
  position: TooltipPosition = 'above';

  constructor(
    private elementRef: ElementRef<HTMLElement>,
    private renderer: Renderer2,
    private tooltip: MatTooltip
  ) {}

  ngOnInit(): void {
    this.applyTruncationStyles();
    this.tooltip.position = this.position;
    this.showTooltipOnOverflow(this);
  }

  ngAfterViewInit(): void {
    this.tooltip.message = this.text || this.elementRef.nativeElement.innerText;
  }

  private showTooltipOnOverflow(ctx: TruncateWithTooltipDirective) {
    ctx.tooltip.show = (function(old) {
      function extendsFunction() {
        if (ctx.tooltipEnabled && ctx.isOverflown()) {
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
