// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Directive, ElementRef, inject, OnDestroy } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Directive({
  selector: '[tbPopoverContentResize]',
  standalone: false
})
export class PopoverContentResizeDirective implements AfterViewInit, OnDestroy {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly popover = inject(TbPopoverComponent, { optional: true });

  private readonly resizeObserver = new ResizeObserver(() => this.popover.updatePosition());
  private readonly mutationObserver = new MutationObserver(() => this.observeChildren());

  ngAfterViewInit(): void {
    if (this.popover) {
      this.observeChildren();
      this.mutationObserver.observe(this.elementRef.nativeElement, {childList: true});
    }
  }

  ngOnDestroy(): void {
    this.mutationObserver.disconnect();
    this.resizeObserver.disconnect();
  }

  private observeChildren(): void {
    this.resizeObserver.disconnect();
    for (const child of Array.from(this.elementRef.nativeElement.children)) {
      this.resizeObserver.observe(child);
    }
  }
}
