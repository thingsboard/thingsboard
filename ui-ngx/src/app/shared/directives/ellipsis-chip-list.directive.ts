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

import {
  Directive,
  ElementRef,
  Inject,
  Input,
  OnDestroy,
  Renderer2,
} from '@angular/core';
import { isEqual } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { WINDOW } from '@core/services/window.service';
import { fromEvent, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: '[tb-ellipsis-chip-list]',
  standalone: true,
})
export class EllipsisChipListDirective implements OnDestroy {

  chipsValue: string[];

  private destroy$ = new Subject<void>();
  private intersectionObserver: IntersectionObserver;

  @Input('tb-ellipsis-chip-list')
  set chips(value: string[]) {
    if (!isEqual(this.chipsValue, value)) {
      this.chipsValue = value;
      setTimeout(() => {
        this.adjustChips();
      }, 0);
    }
  }

  constructor(private el: ElementRef,
              private renderer: Renderer2,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window) {
    this.renderer.setStyle(this.el.nativeElement, 'max-height', '48px');
    this.renderer.setStyle(this.el.nativeElement, 'overflow', 'auto');
    fromEvent(window, 'resize').pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.adjustChips();
    });
    this.observeIntersection();
  }

  private observeIntersection(): void {
    this.intersectionObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          this.adjustChips();
        }
      });
    });

    this.intersectionObserver.observe(this.el.nativeElement);
  }

  private adjustChips(): void {
    const chipListElement = this.el.nativeElement;
    const ellipsisChip = this.el.nativeElement.querySelector('.ellipsis-chip');
    const margin = parseFloat(this.window.getComputedStyle(ellipsisChip).marginLeft) || 0;
    const chipNodes = chipListElement.querySelectorAll('mat-chip:not(.ellipsis-chip)');

    if (this.chipsValue.length > 1) {
      const ellipsisText = this.el.nativeElement.querySelector('.ellipsis-text');
      this.renderer.setStyle(ellipsisChip, 'display', 'inline-flex');
      ellipsisText.innerHTML = this.translate.instant('gateway.ellipsis-chips-text',
        {count: (this.chipsValue.length)});

      const availableWidth = chipListElement.offsetWidth - (ellipsisChip.offsetWidth + margin);
      let usedWidth = 0;
      let visibleChipsCount = 0;

      chipNodes.forEach((chip) => {
        this.renderer.setStyle(chip, 'display', 'inline-flex');
        const textLabelContainer = chip.querySelector('.mdc-evolution-chip__text-label');

        this.applyMaxChipTextWidth(textLabelContainer, (availableWidth / 3));

        if ((usedWidth + (chip.offsetWidth + margin) <= availableWidth) && (visibleChipsCount < this.chipsValue.length)) {
          visibleChipsCount++;
          usedWidth += chip.offsetWidth + margin;
        } else {
          this.renderer.setStyle(chip, 'display', 'none');
        }
      });

      ellipsisText.innerHTML = this.translate.instant('gateway.ellipsis-chips-text',
        {count: (this.chipsValue.length - visibleChipsCount)});

      if (visibleChipsCount === this.chipsValue?.length) {
        this.renderer.setStyle(ellipsisChip, 'display', 'none');
      }
    } else if (this.chipsValue.length === 1) {
      const chipLabelContainer = chipNodes[0].querySelector('.mdc-evolution-chip__action');
      const textLabelContainer = chipLabelContainer.querySelector('.mdc-evolution-chip__text-label');
      const leftPadding = parseFloat(this.window.getComputedStyle(chipLabelContainer).paddingLeft) || 0;
      const rightPadding = parseFloat(this.window.getComputedStyle(chipLabelContainer).paddingRight) || 0;
      const computedTextWidth = chipListElement.offsetWidth - margin -
        (leftPadding + rightPadding);

      this.renderer.setStyle(ellipsisChip, 'display', 'none');
      this.renderer.setStyle(chipNodes[0], 'display', 'inline-flex');

      this.applyMaxChipTextWidth(textLabelContainer, computedTextWidth);
    } else {
      this.renderer.setStyle(ellipsisChip, 'display', 'none');
    }
  }

  private applyMaxChipTextWidth(element: HTMLElement, widthLimit: number): void {
    this.renderer.setStyle(element, 'max-width', widthLimit + 'px');
    this.renderer.setStyle(element, 'overflow', 'hidden');
    this.renderer.setStyle(element, 'text-overflow', 'ellipsis');
    this.renderer.setStyle(element, 'white-space', 'nowrap');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.intersectionObserver.disconnect();
  }
}
