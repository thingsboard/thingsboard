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

import { AfterViewInit, Directive, ElementRef, Input, NgZone, OnDestroy, Renderer2 } from '@angular/core';

@Directive({
  selector: '[tb-chip-overflow]',
  standalone: false
})
export class ChipOverflowDirective implements AfterViewInit, OnDestroy {

  @Input() chipSelector = '.tb-chip';
  @Input() gap = 8;
  @Input() overflowTemplate = '+{n}';
  @Input() overflowClass = 'tb-overflow-chip';
  @Input() minChips = 1;

  private resizeObserver?: ResizeObserver;
  private mutationObserver?: MutationObserver;
  private overflowEl!: HTMLElement;
  private timeoutId?: NodeJS.Timeout;

  constructor(
    private host: ElementRef<HTMLElement>,
    private renderer: Renderer2,
    private zone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.prepareHost();
    this.createOverflowChip();
    this.zone.runOutsideAngular(() => {
      requestAnimationFrame(() => {
        this.reflow();
        this.observeResize();
        this.observeMutations();
      });
    });
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.mutationObserver?.disconnect();
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
  }

  private prepareHost() {
    const el = this.host.nativeElement;
    const styles: { [key: string]: string } = {
      display: 'flex',
      flexWrap: 'nowrap',
      alignItems: 'center',
      overflow: 'hidden',
      gap: `${this.gap}px`,
      whiteSpace: 'nowrap',
      minWidth: '0'
    };
    Object.entries(styles).forEach(([prop, value]) =>
      this.renderer.setStyle(el, prop, value)
    );
  }

  private createOverflowChip() {
    this.overflowEl = this.renderer.createElement('div') as HTMLElement;
    this.renderer.setProperty(this.overflowEl, 'className', this.overflowClass);
    this.renderer.setStyle(this.overflowEl, 'display', 'none');
    this.renderer.setStyle(this.overflowEl, 'userSelect', 'none');
    this.renderer.setStyle(this.overflowEl, 'pointerEvents', 'none');
    this.renderer.appendChild(this.host.nativeElement, this.overflowEl);
  }

  private observeResize(): void {
    this.resizeObserver = new ResizeObserver(() => {
      if (this.timeoutId) {
        clearTimeout(this.timeoutId);
      }
      this.timeoutId = setTimeout(() => this.reflow());
    });
    this.resizeObserver.observe(this.host.nativeElement);
  }

  private observeMutations(): void {
    this.mutationObserver = new MutationObserver(mutations => {
      const relevantMutations = mutations.some(
        mutation =>
          mutation.type === 'childList' &&
          Array.from(mutation.addedNodes).some(node => node instanceof HTMLElement && node.matches(this.chipSelector)) ||
          Array.from(mutation.removedNodes).some(node => node instanceof HTMLElement && node.matches(this.chipSelector))
      );

      if (relevantMutations) {
        if (this.timeoutId) {
          clearTimeout(this.timeoutId);
        }
        this.timeoutId = setTimeout(() => this.reflow());
      }
    });

    this.mutationObserver.observe(this.host.nativeElement, {
      childList: true,
    });
  }

  private reflow() {
    const container = this.host.nativeElement;
    const chips = Array.from(container.querySelectorAll<HTMLElement>(this.chipSelector))
      .filter(el => el !== this.overflowEl);

    if (!chips.length) {
      return;
    }

    // 1. Show all chips to measure their natural widths
    chips.forEach(chip => this.renderer.setStyle(chip, 'display', ''));
    this.renderer.setStyle(this.overflowEl, 'display', 'none');

    const chipWidths = chips.map(chip => Math.ceil(chip.offsetWidth));

    // 2. Hide everything to measure the true available width
    //    without content inflating the table cell
    chips.forEach(chip => this.renderer.setStyle(chip, 'display', 'none'));

    const availableWidth = container.clientWidth;

    if (!availableWidth) {
      chips.forEach(chip => this.renderer.setStyle(chip, 'display', ''));
      return;
    }

    // 3. Measure overflow chip width
    this.renderer.setProperty(this.overflowEl, 'textContent', this.overflowTemplate.replace('{n}', '9'));
    this.renderer.setStyle(this.overflowEl, 'display', 'inline-flex');
    const minOverflowWidth = this.overflowEl.offsetWidth;
    this.renderer.setStyle(this.overflowEl, 'display', 'none');

    // 4. Determine which chips fit
    let usedWidth = 0;
    let hiddenCount = 0;
    const minChips = Math.max(1, this.minChips);

    for (let i = 0; i < chips.length; i++) {
      const chipWidth = chipWidths[i];
      const nextUsedWidth = usedWidth ? usedWidth + this.gap + chipWidth : chipWidth;
      const withOverflow = nextUsedWidth + (nextUsedWidth ? this.gap : 0) + minOverflowWidth;

      if (i < minChips || withOverflow <= availableWidth) {
        this.renderer.setStyle(chips[i], 'display', 'inline-flex');
        usedWidth = nextUsedWidth;
      } else {
        hiddenCount++;
      }
    }

    if (hiddenCount > 0) {
      this.renderer.setProperty(this.overflowEl, 'textContent', this.overflowTemplate.replace('{n}', String(hiddenCount)));
      this.renderer.setStyle(this.overflowEl, 'display', 'inline-flex');
    }
  }
}
