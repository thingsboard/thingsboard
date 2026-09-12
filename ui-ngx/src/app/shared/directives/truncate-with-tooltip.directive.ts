// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        }],
    standalone: false
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
