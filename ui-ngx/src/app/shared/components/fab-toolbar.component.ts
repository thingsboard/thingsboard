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
  AfterViewInit,
  Component,
  Directive,
  ElementRef,
  Inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import { WINDOW } from '@core/services/window.service';
import { _Constructor, CanColor, mixinColor, ThemePalette } from '@angular/material/core';

export declare type FabToolbarDirection = 'left' | 'right';

class MatFabToolbarBase {
  // eslint-disable-next-line @typescript-eslint/naming-convention, no-underscore-dangle, id-blacklist, id-match
  constructor(public _elementRef: ElementRef) {}
}
const MatFabToolbarMixinBase: _Constructor<CanColor> & typeof MatFabToolbarBase = mixinColor(MatFabToolbarBase);

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'mat-fab-trigger',
    standalone: false
})
export class FabTriggerDirective {

  constructor(private el: ElementRef<HTMLElement>) {
  }

}

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'mat-fab-actions',
    standalone: false
})
export class FabActionsDirective implements OnInit {

  constructor(private el: ElementRef<HTMLElement>) {
  }

  ngOnInit(): void {
    const element = $(this.el.nativeElement);
    const children = element.children();
    children.wrap('<div class="mat-fab-action-item">');
  }

}

// @dynamic
@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'mat-fab-toolbar',
    templateUrl: './fab-toolbar.component.html',
    styleUrls: ['./fab-toolbar.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class FabToolbarComponent extends MatFabToolbarMixinBase implements OnInit, OnDestroy, AfterViewInit, OnChanges {

  private fabToolbarResize$: ResizeObserver;

  @Input()
  isOpen: boolean;

  @Input()
  direction: FabToolbarDirection;

  @Input()
  color: ThemePalette;

  constructor(private el: ElementRef<HTMLElement>,
              @Inject(WINDOW) private window: Window) {
    super(el);
  }

  ngOnInit(): void {
    const element = $(this.el.nativeElement);
    element.addClass('mat-fab-toolbar');
    element.find('mat-fab-trigger').find('button')
      .prepend('<div class="mat-fab-toolbar-background"></div>');
    element.addClass(`mat-${this.direction}`);
    this.fabToolbarResize$ = new ResizeObserver(() => {
      this.onFabToolbarResize();
    });
    this.fabToolbarResize$.observe(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.fabToolbarResize$.disconnect();
  }

  ngAfterViewInit(): void {
    this.triggerOpenClose(true);
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'isOpen') {
          this.triggerOpenClose();
        }
      }
    }
  }

  private onFabToolbarResize() {
    if (this.isOpen) {
      this.triggerOpenClose(true);
    }
  }

  private triggerOpenClose(disableAnimation?: boolean): void {
    const el = this.el.nativeElement;
    const element = $(this.el.nativeElement);
    if (disableAnimation) {
      element.removeClass('mat-animation');
    } else {
      element.addClass('mat-animation');
    }
    const backgroundElement: HTMLElement = el.querySelector('.mat-fab-toolbar-background');
    const triggerElement: HTMLElement = el.querySelector('mat-fab-trigger button');
    const toolbarElement: HTMLElement = el.querySelector('mat-toolbar');
    const iconElement: HTMLElement = el.querySelector('mat-fab-trigger button mat-icon');
    const actions = element.find<HTMLElement>('mat-fab-actions').children();
    if (triggerElement && backgroundElement) {
      const width = el.offsetWidth;
      const scale = 2 * (width / triggerElement.offsetWidth);

      backgroundElement.style.borderRadius = width + 'px';

      if (this.isOpen) {
        element.addClass('mat-is-open');
        toolbarElement.style.pointerEvents = 'inherit';

        backgroundElement.style.width = triggerElement.offsetWidth + 'px';
        backgroundElement.style.height = triggerElement.offsetHeight + 'px';
        backgroundElement.style.transform = 'scale(' + scale + ')';

        backgroundElement.style.transitionDelay = '0ms';
        if (iconElement) {
          iconElement.style.transitionDelay = disableAnimation ? '0ms' : '.3s';
        }

        actions.each((index, action) => {
          action.style.transitionDelay = disableAnimation ? '0ms' : ((actions.length - index) * 25 + 'ms');
        });

      } else {
        element.removeClass('mat-is-open');
        toolbarElement.style.pointerEvents = 'none';

        backgroundElement.style.transform = 'scale(1)';

        backgroundElement.style.top = '0';

        if (element.hasClass('mat-right')) {
          backgroundElement.style.left = '0';
          backgroundElement.style.right = null;
        }

        if (element.hasClass('mat-left')) {
          backgroundElement.style.right = '0';
          backgroundElement.style.left = null;
        }

        backgroundElement.style.transitionDelay = disableAnimation ? '0ms' : '200ms';

        actions.each((index, action) => {
          action.style.transitionDelay = (disableAnimation ? 0 : 200) + (index * 25) + 'ms';
        });
      }
    }
  }

}
